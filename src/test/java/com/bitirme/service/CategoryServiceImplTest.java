package com.bitirme.service;

import com.bitirme.dto.category.CategoryCreateRequest;
import com.bitirme.dto.category.CategoryResponse;
import com.bitirme.dto.category.CategoryUpdateRequest;
import com.bitirme.entity.Category;
import com.bitirme.exception.AlreadyExistsException;
import com.bitirme.exception.NotFoundException;
import com.bitirme.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    @DisplayName("create")
    void createTest() {
        CategoryCreateRequest req = new CategoryCreateRequest();
        req.setName("X");
        req.setDescription("d");
        req.setActive(true);
        when(categoryRepository.existsByName("X")).thenReturn(false);
        Category c = new Category();
        c.setId(1);
        c.setName("X");
        c.setDescription("d");
        c.setActive(true);
        when(categoryRepository.save(any(Category.class))).thenReturn(c);

        CategoryResponse r = categoryService.create(req);
        assertThat(r.getName()).isEqualTo("X");
    }

    @Test
    @DisplayName("create: isim çakışması")
    void createDuplicateTest() {
        CategoryCreateRequest req = new CategoryCreateRequest();
        req.setName("Dup");
        req.setDescription("d");
        when(categoryRepository.existsByName("Dup")).thenReturn(true);
        assertThrows(AlreadyExistsException.class, () -> categoryService.create(req));
    }

    @Test
    @DisplayName("getById: yok")
    void getByIdNotFoundTest() {
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> categoryService.getById(99));
    }

    @Test
    @DisplayName("update")
    void updateTest() {
        Category c = new Category();
        c.setId(1);
        c.setName("N1");
        when(categoryRepository.findById(1)).thenReturn(Optional.of(c));
        CategoryUpdateRequest upd = new CategoryUpdateRequest();
        upd.setId(1);
        upd.setName("N2");
        upd.setDescription("d2");
        upd.setActive(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        CategoryResponse out = categoryService.update(1, upd);
        assertThat(out.getName()).isEqualTo("N2");
        verify(categoryRepository).save(any(Category.class));
    }
}
