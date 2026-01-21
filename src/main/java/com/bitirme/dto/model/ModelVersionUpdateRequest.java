package com.bitirme.dto.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Model Versiyonu Güncelleme İsteği")
public class ModelVersionUpdateRequest extends ModelVersionCreateRequest {
    @NotNull(message = "Model versiyonu ID boş olamaz")
    @Schema(description = "Güncellenecek model versiyonu ID'si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;
}
