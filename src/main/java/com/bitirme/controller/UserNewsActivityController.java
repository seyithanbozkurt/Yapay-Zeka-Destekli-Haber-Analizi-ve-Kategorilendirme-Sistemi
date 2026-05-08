package com.bitirme.controller;

import com.bitirme.dto.common.ApiResponse;
import com.bitirme.dto.user.SavedNewsToggleResponse;
import com.bitirme.dto.user.UserReadHistoryResponse;
import com.bitirme.dto.user.UserSavedNewsResponse;
import com.bitirme.service.UserNewsActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "User News Activity", description = "Kullanıcının kaydedilen haberleri ve okuma geçmişi")
public class UserNewsActivityController {

    private final UserNewsActivityService userNewsActivityService;

    @GetMapping("/saved-news")
    @Operation(summary = "Kaydedilen haberleri getir")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Kaydedilen haberler getirildi")
    })
    public ResponseEntity<ApiResponse<List<UserSavedNewsResponse>>> getSavedNews(Authentication authentication) {
        List<UserSavedNewsResponse> data = userNewsActivityService.getSavedNews(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/saved-news/{newsId}/toggle")
    @Operation(summary = "Haberi kaydet/kaydı kaldır")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Kaydetme durumu güncellendi")
    })
    public ResponseEntity<ApiResponse<SavedNewsToggleResponse>> toggleSavedNews(
            Authentication authentication,
            @PathVariable Long newsId
    ) {
        SavedNewsToggleResponse data = userNewsActivityService.toggleSavedNews(authentication.getName(), newsId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/saved-news/{newsId}/status")
    @Operation(summary = "Bir haberin kaydedilme durumunu getir")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Durum getirildi")
    })
    public ResponseEntity<ApiResponse<SavedNewsToggleResponse>> getSavedNewsStatus(
            Authentication authentication,
            @PathVariable Long newsId
    ) {
        boolean saved = userNewsActivityService.isSavedNews(authentication.getName(), newsId);
        return ResponseEntity.ok(ApiResponse.success(new SavedNewsToggleResponse(saved)));
    }

    @DeleteMapping("/saved-news/{newsId}")
    @Operation(summary = "Kaydedilen haberi kaldır")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Kaydedilen haber kaldırıldı")
    })
    public ResponseEntity<ApiResponse<Void>> removeSavedNews(Authentication authentication, @PathVariable Long newsId) {
        userNewsActivityService.removeSavedNews(authentication.getName(), newsId);
        return ResponseEntity.ok(ApiResponse.success("Kaydedilen haber kaldırıldı", null));
    }

    @GetMapping("/read-history")
    @Operation(summary = "Okuma geçmişini getir")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Okuma geçmişi getirildi")
    })
    public ResponseEntity<ApiResponse<List<UserReadHistoryResponse>>> getReadHistory(Authentication authentication) {
        List<UserReadHistoryResponse> data = userNewsActivityService.getReadHistory(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/read-history/{newsId}")
    @Operation(summary = "Haberi okundu olarak işaretle")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Okuma geçmişi güncellendi")
    })
    public ResponseEntity<ApiResponse<Void>> markAsRead(Authentication authentication, @PathVariable Long newsId) {
        userNewsActivityService.markAsRead(authentication.getName(), newsId);
        return ResponseEntity.ok(ApiResponse.success("Okuma geçmişi güncellendi", null));
    }
}

