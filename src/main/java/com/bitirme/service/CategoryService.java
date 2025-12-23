package com.bitirme.service;

import com.bitirme.dto.category.CategoryCreateRequest;
import com.bitirme.dto.category.CategoryResponse;
import com.bitirme.dto.category.CategoryUpdateRequest;

import java.util.List;

public interface CategoryService {
    CategoryResponse create(CategoryCreateRequest request);
    CategoryResponse getById(Integer id);
    List<CategoryResponse> getAll();
    CategoryResponse update(Integer id, CategoryUpdateRequest request);
    void delete(Integer id);
}


