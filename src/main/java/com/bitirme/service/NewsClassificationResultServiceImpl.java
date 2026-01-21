package com.bitirme.service;

import com.bitirme.dto.news.NewsClassificationResultCreateRequest;
import com.bitirme.dto.news.NewsClassificationResultResponse;
import com.bitirme.dto.news.NewsClassificationResultUpdateRequest;
import com.bitirme.entity.Category;
import com.bitirme.entity.ModelVersion;
import com.bitirme.entity.News;
import com.bitirme.entity.NewsClassificationResult;
import com.bitirme.exception.NotFoundException;
import com.bitirme.repository.CategoryRepository;
import com.bitirme.repository.ModelVersionRepository;
import com.bitirme.repository.NewsClassificationResultRepository;
import com.bitirme.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsClassificationResultServiceImpl implements NewsClassificationResultService {

    private final NewsClassificationResultRepository newsClassificationResultRepository;
    private final NewsRepository newsRepository;
    private final ModelVersionRepository modelVersionRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public NewsClassificationResultResponse create(NewsClassificationResultCreateRequest request) {
        News news = newsRepository.findById(request.getNewsId())
                .orElseThrow(() -> new NotFoundException("Haber bulunamadı: " + request.getNewsId()));

        ModelVersion modelVersion = modelVersionRepository.findById(request.getModelVersionId())
                .orElseThrow(() -> new NotFoundException("Model versiyonu bulunamadı: " + request.getModelVersionId()));

        Category category = categoryRepository.findById(request.getPredictedCategoryId())
                .orElseThrow(() -> new NotFoundException("Kategori bulunamadı: " + request.getPredictedCategoryId()));

        NewsClassificationResult result = new NewsClassificationResult();
        result.setNews(news);
        result.setModelVersion(modelVersion);
        result.setPredictedCategory(category);
        result.setPredictionScore(request.getPredictionScore());
        result.setActive(request.getActive() != null ? request.getActive() : true);

        NewsClassificationResult saved = newsClassificationResultRepository.save(result);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public NewsClassificationResultResponse getById(Long id) {
        NewsClassificationResult result = newsClassificationResultRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sınıflandırma sonucu bulunamadı: " + id));
        return toResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsClassificationResultResponse> getAll() {
        return newsClassificationResultRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public NewsClassificationResultResponse update(Long id, NewsClassificationResultUpdateRequest request) {
        NewsClassificationResult result = newsClassificationResultRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sınıflandırma sonucu bulunamadı: " + id));

        if (request.getModelVersionId() != null) {
            ModelVersion modelVersion = modelVersionRepository.findById(request.getModelVersionId())
                    .orElseThrow(() -> new NotFoundException("Model versiyonu bulunamadı: " + request.getModelVersionId()));
            result.setModelVersion(modelVersion);
        }

        if (request.getPredictedCategoryId() != null) {
            Category category = categoryRepository.findById(request.getPredictedCategoryId())
                    .orElseThrow(() -> new NotFoundException("Kategori bulunamadı: " + request.getPredictedCategoryId()));
            result.setPredictedCategory(category);
        }

        if (request.getPredictionScore() != null) {
            result.setPredictionScore(request.getPredictionScore());
        }

        if (request.getActive() != null) {
            result.setActive(request.getActive());
        }

        NewsClassificationResult saved = newsClassificationResultRepository.save(result);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!newsClassificationResultRepository.existsById(id)) {
            throw new NotFoundException("Sınıflandırma sonucu bulunamadı: " + id);
        }
        newsClassificationResultRepository.deleteById(id);
    }

    private NewsClassificationResultResponse toResponse(NewsClassificationResult result) {
        NewsClassificationResultResponse response = new NewsClassificationResultResponse();
        response.setId(result.getId());
        response.setNewsTitle(result.getNews().getTitle());
        response.setModelVersionName(result.getModelVersion().getName());
        response.setPredictedCategoryName(result.getPredictedCategory().getName());
        response.setPredictionScore(result.getPredictionScore());
        response.setActive(result.getActive());
        return response;
    }
}


