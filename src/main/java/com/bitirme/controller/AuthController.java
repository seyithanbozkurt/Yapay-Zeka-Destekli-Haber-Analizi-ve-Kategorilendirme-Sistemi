package com.bitirme.controller;

import com.bitirme.dto.auth.AuthResponse;
import com.bitirme.dto.auth.LoginRequest;
import com.bitirme.dto.common.ApiResponse;
import com.bitirme.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Kimlik doğrulama API endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Giriş yap", description = "Kullanıcı adı ve şifre ile giriş yapar ve JWT token döner. Bu endpoint için yetkilendirme gerekmez.", security = {})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Giriş başarılı"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Kullanıcı adı veya şifre hatalı")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Giriş başarılı", response));
    }
}

