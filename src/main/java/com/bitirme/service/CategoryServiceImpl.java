package com.bitirme.service;

import com.bitirme.dto.category.CategoryCreateRequest;
import com.bitirme.dto.category.CategoryResponse;
import com.bitirme.dto.category.CategoryUpdateRequest;
import com.bitirme.entity.Category;
import com.bitirme.exception.AlreadyExistsException;
import com.bitirme.exception.NotFoundException;
import com.bitirme.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new AlreadyExistsException("Kategori adı zaten kullanılıyor: " + request.getName());
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setActive(request.getActive() != null ? request.getActive() : true);

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kategori bulunamadı: " + id));
        return toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponse update(Integer id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kategori bulunamadı: " + id));

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByName(request.getName())) {
                throw new AlreadyExistsException("Kategori adı zaten kullanılıyor: " + request.getName());
            }
            category.setName(request.getName());
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("Kategori bulunamadı: " + id);
        }
        categoryRepository.deleteById(id);
    }

    private CategoryResponse toResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setActive(category.getActive());
        return response;
    }
}


