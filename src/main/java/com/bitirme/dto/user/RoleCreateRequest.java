package com.bitirme.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Rol Oluşturma İsteği")
public class RoleCreateRequest {
    
    @NotBlank(message = "Rol adı boş olamaz")
    @Size(max = 50, message = "Rol adı en fazla 50 karakter olabilir")
    @Schema(description = "Rol adı", example = "ADMIN", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    
    @Size(max = 255, message = "Açıklama en fazla 255 karakter olabilir")
    @Schema(description = "Rol açıklaması", example = "Yönetici rolü - Tüm yetkilere sahip")
    private String description;
}
