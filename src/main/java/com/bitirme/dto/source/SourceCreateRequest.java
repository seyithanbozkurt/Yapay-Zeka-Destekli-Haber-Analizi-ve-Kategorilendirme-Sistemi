package com.bitirme.dto.source;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Haber Kaynağı Oluşturma İsteği")
public class SourceCreateRequest {
    
    @NotBlank(message = "Kaynak adı boş olamaz")
    @Size(max = 100, message = "Kaynak adı en fazla 100 karakter olabilir")
    @Schema(description = "Haber kaynağı adı", example = "Hürriyet", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    
    @Size(max = 255, message = "Base URL en fazla 255 karakter olabilir")
    @Schema(description = "Haber kaynağı base URL'i", example = "https://www.hurriyet.com.tr")
    private String baseUrl;
    
    @Size(max = 255, message = "Kategori path en fazla 255 karakter olabilir")
    @Schema(description = "Kategori path'i", example = "/gundem")
    private String categoryPath;
    
    @Schema(description = "Kaynak aktiflik durumu", example = "true")
    private Boolean active = true;
    
    @Size(max = 500, message = "Crawl URL en fazla 500 karakter olabilir")
    @Schema(description = "Crawl edilecek URL", example = "https://www.hurriyet.com.tr/gundem/")
    private String crawlUrl;
    
    @Size(max = 200, message = "Title selector en fazla 200 karakter olabilir")
    @Schema(description = "Başlık selector'ı", example = "h2")
    private String titleSelector;
    
    @Size(max = 200, message = "Content selector en fazla 200 karakter olabilir")
    @Schema(description = "İçerik selector'ı", example = "p")
    private String contentSelector;
    
    @Size(max = 200, message = "Link selector en fazla 200 karakter olabilir")
    @Schema(description = "Link selector'ı", example = "a[href*='/gundem/']")
    private String linkSelector;
    
    @Size(max = 50, message = "Crawl type en fazla 50 karakter olabilir")
    @Schema(description = "Crawl tipi", example = "general")
    private String crawlType;
    
    @Size(max = 500, message = "Last minute URL en fazla 500 karakter olabilir")
    @Schema(description = "Son dakika URL'i", example = "https://www.hurriyet.com.tr/son-dakika")
    private String lastMinuteUrl;
}
