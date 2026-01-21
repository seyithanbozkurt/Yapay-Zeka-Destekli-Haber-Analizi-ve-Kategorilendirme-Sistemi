package com.bitirme.dto.news;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Schema(description = "Haber Oluşturma İsteği")
public class NewsCreateRequest {
    
    @NotNull(message = "Kaynak ID boş olamaz")
    @Schema(description = "Haber kaynağı ID'si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sourceId;
    
    @Size(max = 255, message = "External ID en fazla 255 karakter olabilir")
    @Schema(description = "Dış kaynak ID'si", example = "EXT123456")
    private String externalId;
    
    @NotBlank(message = "Başlık boş olamaz")
    @Size(max = 255, message = "Başlık en fazla 255 karakter olabilir")
    @Schema(description = "Haber başlığı", example = "Türkiye'de yeni ekonomi politikaları açıklandı", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    
    @Schema(description = "Haber içeriği", example = "Bugün yapılan açıklamada...")
    private String content;
    
    @Size(max = 500, message = "Original URL en fazla 500 karakter olabilir")
    @Schema(description = "Orijinal haber URL'i", example = "https://www.hurriyet.com.tr/ekonomi/haber")
    private String originalUrl;
    
    @Size(max = 10, message = "Dil kodu en fazla 10 karakter olabilir")
    @Schema(description = "Haber dili", example = "tr")
    private String language = "tr";
    
    @Schema(description = "Yayınlanma tarihi", example = "2024-01-21T10:00:00")
    private LocalDateTime publishedAt;
    
    @Schema(description = "İşlenme durumu", example = "false")
    private Boolean processed = false;
    
    @Schema(description = "Kategori ID'leri", example = "[1, 2, 3]")
    private Set<Integer> categoryIds;
}
