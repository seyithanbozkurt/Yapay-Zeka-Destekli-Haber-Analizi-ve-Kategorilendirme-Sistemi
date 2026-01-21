package com.bitirme.dto.news;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Haber Sınıflandırma Sonucu Bilgileri")
public class NewsClassificationResultResponse {
    @Schema(description = "Sınıflandırma sonucu ID'si", example = "1")
    private Long id;
    
    @Schema(description = "Haber başlığı", example = "Türkiye'de yeni ekonomi politikaları açıklandı")
    private String newsTitle;
    
    @Schema(description = "Model versiyonu adı", example = "v1.0 - Keyword Based Classifier")
    private String modelVersionName;
    
    @Schema(description = "Tahmin edilen kategori adı", example = "Ekonomi")
    private String predictedCategoryName;
    
    @Schema(description = "Tahmin skoru (0.0000 - 1.0000)", example = "0.9500")
    private BigDecimal predictionScore;
    
    @Schema(description = "Aktiflik durumu", example = "true")
    private Boolean active;
}
