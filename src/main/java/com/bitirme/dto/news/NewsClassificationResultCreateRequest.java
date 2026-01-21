package com.bitirme.dto.news;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Haber Sınıflandırma Sonucu Oluşturma İsteği")
public class NewsClassificationResultCreateRequest {
    
    @NotNull(message = "Haber ID boş olamaz")
    @Schema(description = "Haber ID'si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long newsId;
    
    @NotNull(message = "Model versiyonu ID boş olamaz")
    @Schema(description = "Model versiyonu ID'si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer modelVersionId;
    
    @NotNull(message = "Tahmin edilen kategori ID boş olamaz")
    @Schema(description = "Tahmin edilen kategori ID'si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer predictedCategoryId;
    
    @Schema(description = "Tahmin skoru (0.0000 - 1.0000)", example = "0.9500")
    private BigDecimal predictionScore;
    
    @Schema(description = "Aktiflik durumu", example = "true")
    private Boolean active = true;
}
