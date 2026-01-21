package com.bitirme.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Schema(description = "Kullanıcı Oluşturma İsteği")
public class UserCreateRequest {
    
    @NotBlank(message = "Kullanıcı adı boş olamaz")
    @Size(max = 50, message = "Kullanıcı adı en fazla 50 karakter olabilir")
    @Schema(description = "Kullanıcı adı", example = "metehan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;
    
    @NotBlank(message = "E-posta boş olamaz")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @Size(max = 100, message = "E-posta en fazla 100 karakter olabilir")
    @Schema(description = "E-posta adresi", example = "metehan@bitirme.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
    
    @NotBlank(message = "Şifre boş olamaz")
    @Size(max = 255, message = "Şifre hash en fazla 255 karakter olabilir")
    @Schema(description = "Şifre hash değeri", example = "$2a$10$...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String passwordHash;
    
    @Schema(description = "Kullanıcı aktiflik durumu", example = "true")
    private Boolean active = true;
    
    @Schema(description = "Kullanıcı rol ID'leri", example = "[1, 2]")
    private Set<Integer> roleIds;
}
