package com.bitirme.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Kullanıcı giriş bilgileri")
public class LoginRequest {

    @NotBlank(message = "Kullanıcı adı boş olamaz")
    @Schema(description = "Kullanıcı adı (örn: metehan, seyithan)",example = "metehan",requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;
    
    @NotBlank(message = "Şifre boş olamaz")
    @Schema(description = "Kullanıcı şifresi", example = "123123123",requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}

