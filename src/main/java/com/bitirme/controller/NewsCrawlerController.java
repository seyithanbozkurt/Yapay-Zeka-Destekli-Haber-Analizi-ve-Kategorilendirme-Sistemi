package com.bitirme.controller;

import com.bitirme.dto.common.ApiResponse;
import com.bitirme.entity.Source;
import com.bitirme.repository.SourceRepository;
import com.bitirme.service.NewsClassificationService;
import com.bitirme.service.NewsCrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/news-crawler")
@RequiredArgsConstructor
@Tag(name = "News Crawler Management", description = "Haber çekme ve kategorilendirme işlemleri için API endpoints")
public class NewsCrawlerController {

    private final NewsCrawlerService newsCrawlerService;
    private final NewsClassificationService newsClassificationService;
    private final SourceRepository sourceRepository;

    @PostMapping("/crawl")
    @Operation(summary = "Tüm kaynaklardan haber çek", description = "Aktif tüm haber kaynaklarından haberleri çeker")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Haber çekme işlemi başarıyla tamamlandı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    public ResponseEntity<ApiResponse<Map<String, Integer>>> crawl() {
        int count = newsCrawlerService.crawlAllSources();
        return ResponseEntity.ok(ApiResponse.success("Haber çekme işlemi tamamlandı", 
                Map.of("count", count)));
    }

    @PostMapping("/crawl/{sourceId}")
    @Operation(summary = "Belirli bir kaynaktan haber çek", description = "Belirtilen kaynak ID'sinden haberleri çeker")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Haber çekme işlemi başarıyla tamamlandı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Kaynak bulunamadı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    public ResponseEntity<ApiResponse<Map<String, Integer>>> crawl(@PathVariable Integer sourceId) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new com.bitirme.exception.NotFoundException("Kaynak bulunamadı: " + sourceId));
        int count = newsCrawlerService.crawlSource(source);
        return ResponseEntity.ok(ApiResponse.success("Haber çekme işlemi tamamlandı", 
                Map.of("count", count)));
    }

    @PostMapping("/classify")
    @Operation(summary = "Tüm işlenmemiş haberleri kategorilendir", description = "İşlenmemiş tüm haberleri belirtilen model versiyonu ile kategorilendirir")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Kategorilendirme işlemi başarıyla tamamlandı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Model versiyonu bulunamadı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    public ResponseEntity<ApiResponse<Map<String, Integer>>> classify(@RequestParam Integer modelVersionId) {
        int count = newsClassificationService.classifyUnprocessedNews(modelVersionId);
        return ResponseEntity.ok(ApiResponse.success("Kategorilendirme işlemi tamamlandı", 
                Map.of("count", count)));
    }

    @PostMapping("/classify/{newsId}")
    @Operation(summary = "Belirli bir haberi kategorilendir", description = "Belirtilen haber ID'sini kategorilendirir")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Kategorilendirme işlemi başarıyla tamamlandı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Haber veya model versiyonu bulunamadı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    public ResponseEntity<ApiResponse<Void>> classify(
            @PathVariable Long newsId, @RequestParam Integer modelVersionId) {
        newsClassificationService.classifyNews(newsId, modelVersionId);
        return ResponseEntity.ok(ApiResponse.success("Haber başarıyla kategorilendirildi", null));
    }

    @PostMapping("/breaking-news")
    @Operation(summary = "Son dakika haberlerini çek", description = "Son dakika haberleri desteği olan tüm kaynaklardan haberleri çeker")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Son dakika haber çekme işlemi başarıyla tamamlandı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    public ResponseEntity<ApiResponse<Map<String, Integer>>> breakingNews() {
        int count = newsCrawlerService.crawlBreakingNews();
        return ResponseEntity.ok(ApiResponse.success("Son dakika haber çekme işlemi tamamlandı", 
                Map.of("count", count)));
    }
}

