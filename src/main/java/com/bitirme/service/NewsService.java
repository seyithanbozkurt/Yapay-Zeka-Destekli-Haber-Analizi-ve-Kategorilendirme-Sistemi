package com.bitirme.service;

import com.bitirme.dto.news.NewsCreateRequest;
import com.bitirme.dto.news.NewsResponse;
import com.bitirme.dto.news.NewsUpdateRequest;

import java.util.List;

public interface NewsService {
    NewsResponse create(NewsCreateRequest request);
    NewsResponse getById(Long id);
    List<NewsResponse> getAll();
    com.bitirme.dto.news.NewsPageResponse getPaginated(int page, int size);
    NewsResponse update(Long id, NewsUpdateRequest request);
    void delete(Long id);
}


