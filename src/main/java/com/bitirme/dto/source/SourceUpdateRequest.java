package com.bitirme.dto.source;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Haber Kaynağı Güncelleme İsteği")
public class SourceUpdateRequest extends SourceCreateRequest {
    @NotNull(message = "Kaynak ID boş olamaz")
    @Schema(description = "Güncellenecek kaynak ID'si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;
}
