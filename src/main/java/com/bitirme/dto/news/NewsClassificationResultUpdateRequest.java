package com.bitirme.dto.news;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Haber Sınıflandırma Sonucu Güncelleme İsteği")
public class NewsClassificationResultUpdateRequest extends NewsClassificationResultCreateRequest {
    @NotNull(message = "Sınıflandırma sonucu ID boş olamaz")
    @Schema(description = "Güncellenecek sınıflandırma sonucu ID'si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
