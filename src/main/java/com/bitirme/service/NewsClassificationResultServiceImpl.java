package com.bitirme.service;

import com.bitirme.dto.news.NewsClassificationResultCreateRequest;
import com.bitirme.dto.news.NewsClassificationResultResponse;
import com.bitirme.dto.news.NewsClassificationResultUpdateRequest;
import com.bitirme.entity.Category;
import com.bitirme.entity.ModelVersion;
import com.bitirme.entity.News;
import com.bitirme.entity.NewsClassificationResult;
import com.bitirme.repository.CategoryRepository;
import com.bitirme.repository.ModelVersionRepository;
import com.bitirme.repository.NewsClassificationResultRepository;
import com.bitirme.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
                .orElseThrow(() -> new RuntimeException("News not found with id: " + request.getNewsId()));

        ModelVersion modelVersion = modelVersionRepository.findById(request.getModelVersionId())
                .orElseThrow(() -> new RuntimeException("ModelVersion not found with id: " + request.getModelVersionId()));

        Category category = categoryRepository.findById(request.getPredictedCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getPredictedCategoryId()));

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
                .orElseThrow(() -> new RuntimeException("NewsClassificationResult not found with id: " + id));
        return toResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsClassificationResultResponse> getAll() {
        return newsClassificationResultRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NewsClassificationResultResponse update(Long id, NewsClassificationResultUpdateRequest request) {
        NewsClassificationResult result = newsClassificationResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("NewsClassificationResult not found with id: " + id));

        if (request.getModelVersionId() != null) {
            ModelVersion modelVersion = modelVersionRepository.findById(request.getModelVersionId())
                    .orElseThrow(() -> new RuntimeException("ModelVersion not found with id: " + request.getModelVersionId()));
            result.setModelVersion(modelVersion);
        }

        if (request.getPredictedCategoryId() != null) {
            Category category = categoryRepository.findById(request.getPredictedCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getPredictedCategoryId()));
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
            throw new RuntimeException("NewsClassificationResult not found with id: " + id);
        }
        newsClassificationResultRepository.deleteById(id);
    }

    private NewsClassificationResultResponse toResponse(NewsClassificationResult result) {
        NewsClassificationResultResponse response = new NewsClassificationResultResponse();
        response.setId(result.getId());
        response.setNewsId(result.getNews().getId());
        response.setNewsTitle(result.getNews().getTitle());
        response.setModelVersionId(result.getModelVersion().getId());
        response.setModelVersionName(result.getModelVersion().getName());
        response.setPredictedCategoryId(result.getPredictedCategory().getId());
        response.setPredictedCategoryName(result.getPredictedCategory().getName());
        response.setPredictionScore(result.getPredictionScore());
        response.setClassifiedAt(result.getClassifiedAt());
        response.setActive(result.getActive());
        return response;
    }
}


