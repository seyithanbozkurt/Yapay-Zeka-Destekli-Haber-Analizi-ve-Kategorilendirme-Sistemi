package com.bitirme.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Kategori Bilgileri")
public class CategoryResponse {
    @Schema(description = "Kategori ID'si", example = "1")
    private Integer id;
    
    @Schema(description = "Kategori adı", example = "Siyaset")
    private String name;
    
    @Schema(description = "Kategori açıklaması", example = "Siyaset haberleri kategorisi")
    private String description;
    
    @Schema(description = "Aktiflik durumu", example = "true")
    private Boolean active;
}
