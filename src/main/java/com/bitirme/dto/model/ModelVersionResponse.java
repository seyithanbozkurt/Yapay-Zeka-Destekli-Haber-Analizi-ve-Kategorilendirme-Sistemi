package com.bitirme.dto.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Model Versiyonu Bilgileri")
public class ModelVersionResponse {
    @Schema(description = "Model versiyonu ID'si", example = "1")
    private Integer id;
    
    @Schema(description = "Model versiyonu adı", example = "v1.0 - Keyword Based Classifier")
    private String name;
    
    @Schema(description = "Model versiyonu açıklaması", example = "Anahtar kelime tabanlı kategorilendirme modeli")
    private String description;
    
    @Schema(description = "Oluşturan kullanıcı adı", example = "metehan")
    private String createdByUsername;
}
