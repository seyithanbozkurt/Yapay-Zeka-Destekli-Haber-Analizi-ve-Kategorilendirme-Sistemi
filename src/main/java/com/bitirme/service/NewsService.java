package com.bitirme.service;

import com.bitirme.dto.news.NewsCreateRequest;
import com.bitirme.dto.news.NewsResponse;
import com.bitirme.dto.news.NewsUpdateRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface NewsService {
    NewsResponse create(NewsCreateRequest request);
    NewsResponse getById(Long id);
    List<NewsResponse> getAll();
    Page<NewsResponse> getPage(int page, int size, String search, String sourceName, String categoryName);
    NewsResponse update(Long id, NewsUpdateRequest request);
    void delete(Long id);
}


