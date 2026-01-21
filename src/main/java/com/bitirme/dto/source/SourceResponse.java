package com.bitirme.dto.source;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Haber Kaynağı Bilgileri")
public class SourceResponse {
    @Schema(description = "Kaynak ID'si", example = "1")
    private Integer id;
    
    @Schema(description = "Kaynak adı", example = "Hürriyet")
    private String name;
    
    @Schema(description = "Base URL", example = "https://www.hurriyet.com.tr")
    private String baseUrl;
    
    @Schema(description = "Kategori path'i", example = "/gundem")
    private String categoryPath;
    
    @Schema(description = "Aktiflik durumu", example = "true")
    private Boolean active;
    
    @Schema(description = "Crawl URL'i", example = "https://www.hurriyet.com.tr/gundem/")
    private String crawlUrl;
    
    @Schema(description = "Crawl tipi", example = "general")
    private String crawlType;
    
    @Schema(description = "Son dakika URL'i", example = "https://www.hurriyet.com.tr/son-dakika")
    private String lastMinuteUrl;
}
