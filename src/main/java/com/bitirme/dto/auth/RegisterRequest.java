package com.bitirme.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Kullanıcı kayıt bilgileri")
public class RegisterRequest {

    @NotBlank(message = "E-posta boş olamaz")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @Size(max = 100, message = "E-posta en fazla 100 karakter olabilir")
    @Schema(description = "E-posta adresi", example = "kullanici@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Ad boş olamaz")
    @Size(max = 100, message = "Ad en fazla 100 karakter olabilir")
    @Schema(description = "Kullanıcının adı", example = "Metehan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank(message = "Soyad boş olamaz")
    @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir")
    @Schema(description = "Kullanıcının soyadı", example = "Sargın", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @NotNull(message = "Doğum tarihi boş olamaz")
    @Schema(description = "Doğum tarihi", example = "2000-01-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate birthDate;

    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 6, max = 255, message = "Şifre 6-255 karakter arasında olmalıdır")
    @Schema(description = "Şifre", example = "12345678", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
