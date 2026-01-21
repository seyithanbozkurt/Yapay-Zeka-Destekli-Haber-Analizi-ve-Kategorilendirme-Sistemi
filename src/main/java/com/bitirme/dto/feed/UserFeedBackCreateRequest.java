package com.bitirme.dto.feed;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Kullanıcı Geri Bildirimi Oluşturma İsteği")
public class UserFeedBackCreateRequest {
    
    @NotNull(message = "Haber ID boş olamaz")
    @Schema(description = "Haber ID'si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long newsId;
    
    @NotNull(message = "Kullanıcı ID boş olamaz")
    @Schema(description = "Kullanıcı ID'si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;
    
    @Schema(description = "Model versiyonu ID'si", example = "1")
    private Integer modelVersionId;
    
    @Schema(description = "Mevcut tahmin edilen kategori ID'si", example = "2")
    private Integer currentPredictedCategoryId;
    
    @NotNull(message = "Kullanıcı seçili kategori ID boş olamaz")
    @Schema(description = "Kullanıcı tarafından seçilen kategori ID'si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer userSelectedCategoryId;
    
    @NotBlank(message = "Geri bildirim tipi boş olamaz")
    @Size(max = 20, message = "Geri bildirim tipi en fazla 20 karakter olabilir")
    @Schema(description = "Geri bildirim tipi", example = "POSITIVE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String feedbackType;
    
    @Schema(description = "Geri bildirim yorumu", example = "Kategori doğru seçilmiş")
    private String comment;
}
