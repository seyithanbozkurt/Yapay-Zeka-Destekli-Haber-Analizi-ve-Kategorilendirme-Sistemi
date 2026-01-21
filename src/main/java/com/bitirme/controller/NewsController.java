package com.bitirme.controller;

import com.bitirme.dto.common.ApiResponse;
import com.bitirme.dto.news.NewsCreateRequest;
import com.bitirme.dto.news.NewsResponse;
import com.bitirme.dto.news.NewsUpdateRequest;
import com.bitirme.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Tag(name = "News Management", description = "Haber yönetimi için API endpoints")
public class NewsController {

    private final NewsService newsService;

    @PostMapping
    @Operation(summary = "Yeni haber oluştur", description = "Sistemde yeni bir haber kaydı oluşturur")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Haber başarıyla oluşturuldu"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Geçersiz istek"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Kaynak veya kategori bulunamadı")
    })
    public ResponseEntity<ApiResponse<NewsResponse>> create(@RequestBody NewsCreateRequest request) {
        NewsResponse response = newsService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Haber başarıyla oluşturuldu", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Haber getir", description = "ID'ye göre haber bilgilerini getirir")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Haber bulundu"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Haber bulunamadı")
    })
    public ResponseEntity<ApiResponse<NewsResponse>> getById(@PathVariable Long id) {
        NewsResponse response = newsService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Tüm haberleri listele", description = "Sistemdeki tüm haberleri listeler")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Haber listesi başarıyla getirildi")
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getAll() {
        List<NewsResponse> responses = newsService.getAll();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping
    @Operation(summary = "Haber güncelle", description = "Mevcut bir haberin bilgilerini günceller. ID request body'den alınır.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Haber başarıyla güncellendi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Haber bulunamadı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Geçersiz istek")
    })
    public ResponseEntity<ApiResponse<NewsResponse>> update(@RequestBody NewsUpdateRequest request) {
        NewsResponse response = newsService.update(request.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Haber başarıyla güncellendi", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Haber sil", description = "ID'ye göre haberi siler")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Haber başarıyla silindi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Haber bulunamadı")
    })
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        newsService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Haber başarıyla silindi", null));
    }
}

