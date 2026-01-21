package com.bitirme.controller;

import com.bitirme.dto.category.CategoryCreateRequest;
import com.bitirme.dto.category.CategoryResponse;
import com.bitirme.dto.category.CategoryUpdateRequest;
import com.bitirme.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "Kategori yönetimi için API endpoints")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Yeni kategori oluştur", description = "Sistemde yeni bir kategori kaydı oluşturur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Kategori başarıyla oluşturuldu"),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek"),
            @ApiResponse(responseCode = "409", description = "Kategori adı zaten kullanılıyor")
    })
    public ResponseEntity<CategoryResponse> create(@RequestBody CategoryCreateRequest request) {
        CategoryResponse response = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Kategori getir", description = "ID'ye göre kategori bilgilerini getirir")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Kategori bulundu"),
            @ApiResponse(responseCode = "404", description = "Kategori bulunamadı")
    })
    public ResponseEntity<CategoryResponse> getById(@PathVariable Integer id) {
        CategoryResponse response = categoryService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Tüm kategorileri listele", description = "Sistemdeki tüm kategorileri listeler")
    @ApiResponse(responseCode = "200", description = "Kategori listesi başarıyla getirildi")
    public ResponseEntity<List<CategoryResponse>> getAll() {
        List<CategoryResponse> responses = categoryService.getAll();
        return ResponseEntity.ok(responses);
    }

    @PutMapping
    @Operation(summary = "Kategori güncelle", description = "Mevcut bir kategorinin bilgilerini günceller. ID request body'den alınır.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Kategori başarıyla güncellendi"),
            @ApiResponse(responseCode = "404", description = "Kategori bulunamadı"),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek")
    })
    public ResponseEntity<CategoryResponse> update(@RequestBody CategoryUpdateRequest request) {
        CategoryResponse response = categoryService.update(request.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Kategori sil", description = "ID'ye göre kategoriyi siler")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Kategori başarıyla silindi"),
            @ApiResponse(responseCode = "404", description = "Kategori bulunamadı")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

