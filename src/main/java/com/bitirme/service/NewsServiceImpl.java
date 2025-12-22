package com.bitirme.service;

import com.bitirme.dto.news.NewsCreateRequest;
import com.bitirme.dto.news.NewsResponse;
import com.bitirme.dto.news.NewsUpdateRequest;
import com.bitirme.entity.Category;
import com.bitirme.entity.News;
import com.bitirme.entity.Source;
import com.bitirme.repository.CategoryRepository;
import com.bitirme.repository.NewsRepository;
import com.bitirme.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
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

        News news = new News();
        news.setSource(source);
        news.setExternalId(request.getExternalId());
        news.setTitle(request.getTitle());
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
                .collect(Collectors.toList());
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
            news.setTitle(request.getTitle());
        }

        if (request.getContent() != null) {
            news.setContent(request.getContent());
        }

        if (request.getOriginalUrl() != null) {
            news.setOriginalUrl(request.getOriginalUrl());
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
        newsRepository.deleteById(id);
    }

    private NewsResponse toResponse(News news) {
        NewsResponse response = new NewsResponse();
        response.setId(news.getId());
        response.setSourceId(news.getSource().getId());
        response.setSourceName(news.getSource().getName());
        response.setExternalId(news.getExternalId());
        response.setTitle(news.getTitle());
        response.setContent(news.getContent());
        response.setOriginalUrl(news.getOriginalUrl());
        response.setLanguage(news.getLanguage());
        response.setPublishedAt(news.getPublishedAt());
        response.setFetchedAt(news.getFetchedAt());
        response.setProcessed(news.getProcessed());
        response.setCreatedAt(news.getCreatedAt());
        response.setUpdatedAt(news.getUpdatedAt());
        response.setCategoryIds(news.getCategories().stream()
                .map(Category::getId)
                .collect(Collectors.toSet()));
        response.setCategoryNames(news.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toSet()));
        return response;
    }
}

