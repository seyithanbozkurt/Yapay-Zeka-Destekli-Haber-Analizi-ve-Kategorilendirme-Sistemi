package com.bitirme.service;

import com.bitirme.dto.news.NewsCreateRequest;
import com.bitirme.dto.news.NewsResponse;
import com.bitirme.dto.news.NewsUpdateRequest;
import com.bitirme.entity.Category;
import com.bitirme.entity.News;
import com.bitirme.entity.Source;
import com.bitirme.repository.CategoryRepository;
import com.bitirme.repository.NewsRepository;
import com.bitirme.exception.AlreadyExistsException;
import com.bitirme.repository.SourceRepository;
import com.bitirme.util.NewsTitleNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final SourceRepository sourceRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public NewsResponse create(NewsCreateRequest request) {
        Source source = sourceRepository.findById(request.getSourceId())
                .orElseThrow(() -> new com.bitirme.exception.NotFoundException("Kaynak bulunamadı: " + request.getSourceId()));

        request.setExternalId(truncate(request.getExternalId(), 255));
        request.setTitle(truncate(request.getTitle(), 255));
        request.setOriginalUrl(truncate(request.getOriginalUrl(), 500));

        // Aynı kaynaktan aynı başlıkta haber zaten varsa ekleme (API ve crawler tutarlılığı)
        String normalizedTitle = NewsTitleNormalizer.normalize(request.getTitle());
        if (!normalizedTitle.isEmpty() && normalizedTitle.length() >= 5
                && newsRepository.existsBySourceIdAndNormalizedTitle(source.getId(), normalizedTitle)) {
            throw new AlreadyExistsException("Bu kaynakta aynı başlıkta haber zaten mevcut: " + request.getTitle());
        }

        News news = new News();
        news.setSource(source);
        news.setExternalId(request.getExternalId());
        news.setTitle(request.getTitle());
        news.setNormalizedTitle(normalizedTitle);
        news.setContent(request.getContent());
        news.setOriginalUrl(request.getOriginalUrl());
        news.setLanguage(request.getLanguage() != null ? request.getLanguage() : "tr");
        news.setPublishedAt(request.getPublishedAt());
        news.setProcessed(request.getProcessed() != null ? request.getProcessed() : false);

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = request.getCategoryIds().stream()
                    .map(categoryRepository::findById)
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
                    .collect(Collectors.toSet());
            news.setCategories(categories);
        }

        News saved = newsRepository.save(news);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public NewsResponse getById(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new com.bitirme.exception.NotFoundException("Haber bulunamadı: " + id));
        return toResponse(news);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsResponse> getAll() {
        return newsRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public NewsResponse update(Long id, NewsUpdateRequest request) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new com.bitirme.exception.NotFoundException("Haber bulunamadı: " + id));

        if (request.getSourceId() != null) {
            Source source = sourceRepository.findById(request.getSourceId())
                    .orElseThrow(() -> new com.bitirme.exception.NotFoundException("Kaynak bulunamadı: " + request.getSourceId()));
            news.setSource(source);
        }

        if (request.getExternalId() != null) {
            news.setExternalId(request.getExternalId());
        }

        if (request.getTitle() != null) {
            String safeTitle = truncate(request.getTitle(), 255);
            news.setTitle(safeTitle);
            news.setNormalizedTitle(NewsTitleNormalizer.normalize(safeTitle));
        }

        if (request.getContent() != null) {
            news.setContent(request.getContent());
        }

        if (request.getOriginalUrl() != null) {
            news.setOriginalUrl(truncate(request.getOriginalUrl(), 500));
        }

        if (request.getLanguage() != null) {
            news.setLanguage(request.getLanguage());
        }

        if (request.getPublishedAt() != null) {
            news.setPublishedAt(request.getPublishedAt());
        }

        if (request.getProcessed() != null) {
            news.setProcessed(request.getProcessed());
        }

        if (request.getCategoryIds() != null) {
            Set<Category> categories = request.getCategoryIds().stream()
                    .map(categoryRepository::findById)
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
                    .collect(Collectors.toSet());
            news.setCategories(categories);
        }

        News saved = newsRepository.save(news);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!newsRepository.existsById(id)) {
            throw new com.bitirme.exception.NotFoundException("Haber bulunamadı: " + id);
        }
        try {
            newsRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new com.bitirme.exception.BusinessException(
                    "Bu habere bağlı sınıflandırma sonuçları veya kullanıcı geri bildirimleri var. Önce bunları kaldırmanız gerekiyor.");
        }
    }

    private NewsResponse toResponse(News news) {
        NewsResponse response = new NewsResponse();
        response.setId(news.getId());
        response.setSourceName(news.getSource().getName());
        response.setTitle(news.getTitle());
        response.setContent(news.getContent());
        response.setOriginalUrl(news.getOriginalUrl());
        response.setLanguage(news.getLanguage());
        response.setPublishedAt(news.getPublishedAt());
        response.setProcessed(news.getProcessed());
        response.setCategoryNames(news.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toSet()));
        return response;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}

