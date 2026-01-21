package com.bitirme.controller;

import com.bitirme.dto.feed.UserFeedBackCreateRequest;
import com.bitirme.dto.feed.UserFeedBackResponse;
import com.bitirme.dto.feed.UserFeedBackUpdateRequest;
import com.bitirme.service.UserFeedBackService;
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
@RequestMapping("/api/user-feedback")
@RequiredArgsConstructor
@Tag(name = "User Feedback Management", description = "Kullanıcı geri bildirimi yönetimi için API endpoints")
public class UserFeedBackController {

    private final UserFeedBackService userFeedBackService;

    @PostMapping
    @Operation(summary = "Yeni kullanıcı geri bildirimi oluştur", description = "Sistemde yeni bir kullanıcı geri bildirimi kaydı oluşturur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Kullanıcı geri bildirimi başarıyla oluşturuldu"),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek"),
            @ApiResponse(responseCode = "404", description = "Haber, kullanıcı, model versiyonu veya kategori bulunamadı")
    })
    public ResponseEntity<UserFeedBackResponse> create(@RequestBody UserFeedBackCreateRequest request) {
        UserFeedBackResponse response = userFeedBackService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Kullanıcı geri bildirimi getir", description = "ID'ye göre kullanıcı geri bildirimi bilgilerini getirir")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Kullanıcı geri bildirimi bulundu"),
            @ApiResponse(responseCode = "404", description = "Kullanıcı geri bildirimi bulunamadı")
    })
    public ResponseEntity<UserFeedBackResponse> getById(@PathVariable Long id) {
        UserFeedBackResponse response = userFeedBackService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Tüm kullanıcı geri bildirimlerini listele", description = "Sistemdeki tüm kullanıcı geri bildirimlerini listeler")
    @ApiResponse(responseCode = "200", description = "Kullanıcı geri bildirimi listesi başarıyla getirildi")
    public ResponseEntity<List<UserFeedBackResponse>> getAll() {
        List<UserFeedBackResponse> responses = userFeedBackService.getAll();
        return ResponseEntity.ok(responses);
    }

    @PutMapping
    @Operation(summary = "Kullanıcı geri bildirimi güncelle", description = "Mevcut bir kullanıcı geri bildiriminin bilgilerini günceller. ID request body'den alınır.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Kullanıcı geri bildirimi başarıyla güncellendi"),
            @ApiResponse(responseCode = "404", description = "Kullanıcı geri bildirimi bulunamadı"),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek")
    })
    public ResponseEntity<UserFeedBackResponse> update(@RequestBody UserFeedBackUpdateRequest request) {
        UserFeedBackResponse response = userFeedBackService.update(request.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Kullanıcı geri bildirimi sil", description = "ID'ye göre kullanıcı geri bildirimini siler")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Kullanıcı geri bildirimi başarıyla silindi"),
            @ApiResponse(responseCode = "404", description = "Kullanıcı geri bildirimi bulunamadı")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userFeedBackService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

