package com.bitirme.controller;

import com.bitirme.dto.common.ApiResponse;
import com.bitirme.dto.crawl.CrawlingLogResponse;
import com.bitirme.entity.CrawlingLog;
import com.bitirme.repository.CrawlingLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/crawling-logs")
@RequiredArgsConstructor
@Tag(name = "Crawling Log Management", description = "Haber çekme süreçlerinin log kayıtları için API endpoints")
public class CrawlingLogController {

    private final CrawlingLogRepository crawlingLogRepository;

    @GetMapping
    @Operation(summary = "Tüm tarama loglarını listele")
    public ResponseEntity<ApiResponse<List<CrawlingLogResponse>>> getAllLogs() {
        List<CrawlingLogResponse> logs = crawlingLogRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/source/{sourceId}")
    @Operation(summary = "Belirli kaynak için tarama loglarını listele")
    public ResponseEntity<ApiResponse<List<CrawlingLogResponse>>> getLogsBySource(@PathVariable Integer sourceId) {
        List<CrawlingLogResponse> logs = crawlingLogRepository.findBySourceId(sourceId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Duruma göre tarama loglarını listele (SUCCESS, FAILED, RUNNING)")
    public ResponseEntity<ApiResponse<List<CrawlingLogResponse>>> getLogsByStatus(@PathVariable String status) {
        List<CrawlingLogResponse> logs = crawlingLogRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    private CrawlingLogResponse toResponse(CrawlingLog log) {
        return CrawlingLogResponse.builder()
                .sourceName(log.getSource() != null ? log.getSource().getName() : null)
                .startedAt(log.getStartedAt())
                .finishedAt(log.getFinishedAt())
                .status(log.getStatus())
                .fetchedCount(log.getFetchedCount())
                .errorMessage(log.getErrorMessage())
                .build();
    }
}


