package com.bitirme.dto.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Model Versiyonu Oluşturma İsteği")
public class ModelVersionCreateRequest {
    
    @NotBlank(message = "Model versiyonu adı boş olamaz")
    @Size(max = 100, message = "Model versiyonu adı en fazla 100 karakter olabilir")
    @Schema(description = "Model versiyonu adı", example = "v1.0 - Keyword Based Classifier", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    
    @Schema(description = "Model versiyonu açıklaması", example = "Anahtar kelime tabanlı kategorilendirme modeli")
    private String description;
    
    @Schema(description = "Oluşturan kullanıcı ID'si", example = "1")
    private Long createdById;
}
