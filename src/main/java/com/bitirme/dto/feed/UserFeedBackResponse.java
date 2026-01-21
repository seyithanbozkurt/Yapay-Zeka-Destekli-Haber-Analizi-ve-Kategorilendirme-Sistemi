package com.bitirme.dto.feed;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Kullanıcı Geri Bildirimi Bilgileri")
public class UserFeedBackResponse {
    @Schema(description = "Geri bildirim ID'si", example = "1")
    private Long id;
    
    @Schema(description = "Haber başlığı", example = "Türkiye'de yeni ekonomi politikaları açıklandı")
    private String newsTitle;
    
    @Schema(description = "Kullanıcı adı", example = "metehan")
    private String username;
    
    @Schema(description = "Model versiyonu adı", example = "v1.0 - Keyword Based Classifier")
    private String modelVersionName;
    
    @Schema(description = "Mevcut tahmin edilen kategori adı", example = "Ekonomi")
    private String currentPredictedCategoryName;
    
    @Schema(description = "Kullanıcının seçtiği kategori adı", example = "Siyaset")
    private String userSelectedCategoryName;
    
    @Schema(description = "Geri bildirim tipi", example = "POSITIVE")
    private String feedbackType;
    
    @Schema(description = "Geri bildirim yorumu", example = "Kategori doğru seçilmiş")
    private String comment;
}
