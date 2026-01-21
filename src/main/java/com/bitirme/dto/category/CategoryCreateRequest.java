package com.bitirme.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Kategori Oluşturma İsteği")
public class CategoryCreateRequest {

    @NotBlank(message = "Kategori adı boş olamaz")
    @Size(max = 50, message = "Kategori adı en fazla 50 karakter olabilir")
    @Schema(description = "Kategori adı", example = "Siyaset", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Açıklama Kısmı Boş Olamaz")
    @Size(max = 255, message = "Açıklama en fazla 255 karakter olabilir")
    @Schema(description = "Açıklama kısmı", example = "Siyaset haberleri kategorisi", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @Schema(description = "Kategori aktiflik durumu", example = "true")
    private Boolean active = true;
}
