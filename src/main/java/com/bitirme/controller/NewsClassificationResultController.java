package com.bitirme.controller;

import com.bitirme.dto.common.ApiResponse;
import com.bitirme.dto.news.NewsClassificationResultCreateRequest;
import com.bitirme.dto.news.NewsClassificationResultResponse;
import com.bitirme.dto.news.NewsClassificationResultUpdateRequest;
import com.bitirme.service.NewsClassificationResultService;
import com.bitirme.service.NewsClassificationResultServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news-classification-results")
@RequiredArgsConstructor
@Tag(name = "News Classification Result Management", description = "Haber sınıflandırma sonuçları yönetimi için API endpoints")
public class NewsClassificationResultController {

    private final NewsClassificationResultService newsClassificationResultService;
    private final NewsClassificationResultServiceImpl newsClassificationResultServiceImpl;

    @PostMapping
    @Operation(summary = "Yeni sınıflandırma sonucu oluştur", description = "Sistemde yeni bir haber sınıflandırma sonucu kaydı oluşturur")
    public ResponseEntity<NewsClassificationResultResponse> create(@RequestBody NewsClassificationResultCreateRequest request) {
        NewsClassificationResultResponse response = newsClassificationResultService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Sınıflandırma sonucu getir", description = "ID'ye göre sınıflandırma sonucu bilgilerini getirir")
    public ResponseEntity<NewsClassificationResultResponse> getById(@PathVariable Long id) {
        NewsClassificationResultResponse response = newsClassificationResultService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Tüm sınıflandırma sonuçlarını listele", description = "Sistemdeki tüm sınıflandırma sonuçlarını listeler")
    public ResponseEntity<List<NewsClassificationResultResponse>> getAll() {
        List<NewsClassificationResultResponse> responses = newsClassificationResultService.getAll();
        return ResponseEntity.ok(responses);
    }

    @PutMapping
    @Operation(summary = "Sınıflandırma sonucu güncelle", description = "Mevcut bir sınıflandırma sonucunun bilgilerini günceller. ID request body'den alınır.")
    public ResponseEntity<NewsClassificationResultResponse> update(@RequestBody NewsClassificationResultUpdateRequest request) {
        NewsClassificationResultResponse response = newsClassificationResultService.update(request.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Sınıflandırma sonucu sil", description = "ID'ye göre sınıflandırma sonucunu siler")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        newsClassificationResultService.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/by-news/{newsId}")
    @Operation(summary = "Habere göre sınıflandırma sonuçlarını listele", description = "Verilen haber ID'sine ait tüm sınıflandırma sonuçlarını döner")
    public ResponseEntity<List<NewsClassificationResultResponse>> getByNewsId(@PathVariable Long newsId) {
        List<NewsClassificationResultResponse> responses = newsClassificationResultService.getByNewsId(newsId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/backfill-news-categories")
    @Operation(summary = "Mevcut sınıflandırma sonuçlarından news_categories tablosunu doldur")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> backfillNewsCategories() {
        int linked = newsClassificationResultServiceImpl.backfillNewsCategories();
        return ResponseEntity.ok(ApiResponse.success(
                "news_categories backfill tamamlandı",
                Map.of("linkedCount", linked)
        ));
    }
}

