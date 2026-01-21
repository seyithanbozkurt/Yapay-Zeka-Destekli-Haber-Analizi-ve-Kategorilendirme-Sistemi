package com.bitirme.controller;

import com.bitirme.dto.news.NewsClassificationResultCreateRequest;
import com.bitirme.dto.news.NewsClassificationResultResponse;
import com.bitirme.dto.news.NewsClassificationResultUpdateRequest;
import com.bitirme.service.NewsClassificationResultService;
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
@RequestMapping("/api/news-classification-results")
@RequiredArgsConstructor
@Tag(name = "News Classification Result Management", description = "Haber sınıflandırma sonuçları yönetimi için API endpoints")
public class NewsClassificationResultController {

    private final NewsClassificationResultService newsClassificationResultService;

    @PostMapping
    @Operation(summary = "Yeni sınıflandırma sonucu oluştur", description = "Sistemde yeni bir haber sınıflandırma sonucu kaydı oluşturur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sınıflandırma sonucu başarıyla oluşturuldu"),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek"),
            @ApiResponse(responseCode = "404", description = "Haber, model versiyonu veya kategori bulunamadı")
    })
    public ResponseEntity<NewsClassificationResultResponse> create(@RequestBody NewsClassificationResultCreateRequest request) {
        NewsClassificationResultResponse response = newsClassificationResultService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Sınıflandırma sonucu getir", description = "ID'ye göre sınıflandırma sonucu bilgilerini getirir")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sınıflandırma sonucu bulundu"),
            @ApiResponse(responseCode = "404", description = "Sınıflandırma sonucu bulunamadı")
    })
    public ResponseEntity<NewsClassificationResultResponse> getById(@PathVariable Long id) {
        NewsClassificationResultResponse response = newsClassificationResultService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Tüm sınıflandırma sonuçlarını listele", description = "Sistemdeki tüm sınıflandırma sonuçlarını listeler")
    @ApiResponse(responseCode = "200", description = "Sınıflandırma sonucu listesi başarıyla getirildi")
    public ResponseEntity<List<NewsClassificationResultResponse>> getAll() {
        List<NewsClassificationResultResponse> responses = newsClassificationResultService.getAll();
        return ResponseEntity.ok(responses);
    }

    @PutMapping
    @Operation(summary = "Sınıflandırma sonucu güncelle", description = "Mevcut bir sınıflandırma sonucunun bilgilerini günceller. ID request body'den alınır.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sınıflandırma sonucu başarıyla güncellendi"),
            @ApiResponse(responseCode = "404", description = "Sınıflandırma sonucu bulunamadı"),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek")
    })
    public ResponseEntity<NewsClassificationResultResponse> update(@RequestBody NewsClassificationResultUpdateRequest request) {
        NewsClassificationResultResponse response = newsClassificationResultService.update(request.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Sınıflandırma sonucu sil", description = "ID'ye göre sınıflandırma sonucunu siler")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Sınıflandırma sonucu başarıyla silindi"),
            @ApiResponse(responseCode = "404", description = "Sınıflandırma sonucu bulunamadı")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        newsClassificationResultService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

