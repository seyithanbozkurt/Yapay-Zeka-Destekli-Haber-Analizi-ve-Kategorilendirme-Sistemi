package com.bitirme.dto.news;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Haber Güncelleme İsteği")
public class NewsUpdateRequest extends NewsCreateRequest {
    @NotNull(message = "Haber ID boş olamaz")
    @Schema(description = "Güncellenecek haber ID'si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
