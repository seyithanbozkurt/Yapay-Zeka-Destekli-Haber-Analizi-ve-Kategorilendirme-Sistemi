package com.bitirme.service;

import com.bitirme.dto.news.NewsClassificationResultCreateRequest;
import com.bitirme.dto.news.NewsClassificationResultResponse;
import com.bitirme.dto.news.NewsClassificationResultUpdateRequest;

import java.util.List;

public interface NewsClassificationResultService {
    NewsClassificationResultResponse create(NewsClassificationResultCreateRequest request);
    NewsClassificationResultResponse getById(Long id);
    List<NewsClassificationResultResponse> getAll();
    NewsClassificationResultResponse update(Long id, NewsClassificationResultUpdateRequest request);
    void delete(Long id);
}


