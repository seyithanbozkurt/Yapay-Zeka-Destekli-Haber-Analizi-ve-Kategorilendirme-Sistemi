package com.bitirme.controller;

import com.bitirme.dto.model.ModelVersionCreateRequest;
import com.bitirme.dto.model.ModelVersionResponse;
import com.bitirme.dto.model.ModelVersionUpdateRequest;
import com.bitirme.service.ModelVersionService;
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
@RequestMapping("/api/model-versions")
@RequiredArgsConstructor
@Tag(name = "Model Version Management", description = "Model versiyonu yönetimi için API endpoints")
public class ModelVersionController {

    private final ModelVersionService modelVersionService;

    @PostMapping
    @Operation(summary = "Yeni model versiyonu oluştur", description = "Sistemde yeni bir model versiyonu kaydı oluşturur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Model versiyonu başarıyla oluşturuldu"),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek"),
            @ApiResponse(responseCode = "404", description = "Kullanıcı bulunamadı")
    })
    public ResponseEntity<ModelVersionResponse> create(@RequestBody ModelVersionCreateRequest request) {
        ModelVersionResponse response = modelVersionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Model versiyonu getir", description = "ID'ye göre model versiyonu bilgilerini getirir")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Model versiyonu bulundu"),
            @ApiResponse(responseCode = "404", description = "Model versiyonu bulunamadı")
    })
    public ResponseEntity<ModelVersionResponse> getById(@PathVariable Integer id) {
        ModelVersionResponse response = modelVersionService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Tüm model versiyonlarını listele", description = "Sistemdeki tüm model versiyonlarını listeler")
    @ApiResponse(responseCode = "200", description = "Model versiyonu listesi başarıyla getirildi")
    public ResponseEntity<List<ModelVersionResponse>> getAll() {
        List<ModelVersionResponse> responses = modelVersionService.getAll();
        return ResponseEntity.ok(responses);
    }

    @PutMapping
    @Operation(summary = "Model versiyonu güncelle", description = "Mevcut bir model versiyonunun bilgilerini günceller. ID request body'den alınır.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Model versiyonu başarıyla güncellendi"),
            @ApiResponse(responseCode = "404", description = "Model versiyonu bulunamadı"),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek")
    })
    public ResponseEntity<ModelVersionResponse> update(@RequestBody ModelVersionUpdateRequest request) {
        ModelVersionResponse response = modelVersionService.update(request.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Model versiyonu sil", description = "ID'ye göre model versiyonunu siler")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Model versiyonu başarıyla silindi"),
            @ApiResponse(responseCode = "404", description = "Model versiyonu bulunamadı")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        modelVersionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

