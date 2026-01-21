package com.bitirme.dto.news;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Schema(description = "Haber Bilgileri")
public class NewsResponse {
    @Schema(description = "Haber ID'si", example = "1")
    private Long id;
    
    @Schema(description = "Haber kaynağı adı", example = "Hürriyet")
    private String sourceName;
    
    @Schema(description = "Haber başlığı", example = "Türkiye'de yeni ekonomi politikaları açıklandı")
    private String title;
    
    @Schema(description = "Haber içeriği", example = "Bugün yapılan açıklamada...")
    private String content;
    
    @Schema(description = "Orijinal haber URL'i", example = "https://www.hurriyet.com.tr/ekonomi/haber")
    private String originalUrl;
    
    @Schema(description = "Haber dili", example = "tr")
    private String language;
    
    @Schema(description = "Yayınlanma tarihi", example = "2024-01-21T10:00:00")
    private LocalDateTime publishedAt;
    
    @Schema(description = "İşlenme durumu", example = "false")
    private Boolean processed;
    
    @Schema(description = "Kategori isimleri", example = "[\"Ekonomi\", \"Siyaset\"]")
    private Set<String> categoryNames;
}
