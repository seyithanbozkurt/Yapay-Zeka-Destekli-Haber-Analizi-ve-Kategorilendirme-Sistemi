package com.bitirme.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Rol Güncelleme İsteği")
public class RoleUpdateRequest extends RoleCreateRequest {
    @NotNull(message = "Rol ID boş olamaz")
    @Schema(description = "Güncellenecek rol ID'si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;
}
