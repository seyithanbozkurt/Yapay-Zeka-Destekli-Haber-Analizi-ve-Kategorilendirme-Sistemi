package com.bitirme.controller;

import com.bitirme.dto.source.SourceCreateRequest;
import com.bitirme.dto.source.SourceResponse;
import com.bitirme.dto.source.SourceUpdateRequest;
import com.bitirme.service.SourceService;
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
@RequestMapping("/api/sources")
@RequiredArgsConstructor
@Tag(name = "Source Management", description = "Haber kaynağı yönetimi için API endpoints")
public class SourceController {

    private final SourceService sourceService;

    @PostMapping
    @Operation(summary = "Yeni haber kaynağı oluştur", description = "Sistemde yeni bir haber kaynağı kaydı oluşturur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Haber kaynağı başarıyla oluşturuldu"),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek"),
            @ApiResponse(responseCode = "409", description = "Haber kaynağı adı zaten kullanılıyor")
    })
    public ResponseEntity<SourceResponse> create(@RequestBody SourceCreateRequest request) {
        SourceResponse response = sourceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Haber kaynağı getir", description = "ID'ye göre haber kaynağı bilgilerini getirir")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Haber kaynağı bulundu"),
            @ApiResponse(responseCode = "404", description = "Haber kaynağı bulunamadı")
    })
    public ResponseEntity<SourceResponse> getById(@PathVariable Integer id) {
        SourceResponse response = sourceService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Tüm haber kaynaklarını listele", description = "Sistemdeki tüm haber kaynaklarını listeler")
    @ApiResponse(responseCode = "200", description = "Haber kaynağı listesi başarıyla getirildi")
    public ResponseEntity<List<SourceResponse>> getAll() {
        List<SourceResponse> responses = sourceService.getAll();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Haber kaynağı güncelle", description = "Mevcut bir haber kaynağının bilgilerini günceller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Haber kaynağı başarıyla güncellendi"),
            @ApiResponse(responseCode = "404", description = "Haber kaynağı bulunamadı"),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek")
    })
    public ResponseEntity<SourceResponse> update(@PathVariable Integer id, @RequestBody SourceUpdateRequest request) {
        SourceResponse response = sourceService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Haber kaynağı sil", description = "ID'ye göre haber kaynağını siler")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Haber kaynağı başarıyla silindi"),
            @ApiResponse(responseCode = "404", description = "Haber kaynağı bulunamadı")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        sourceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

