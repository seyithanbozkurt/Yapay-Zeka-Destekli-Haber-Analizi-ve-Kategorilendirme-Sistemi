package com.bitirme.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Kategori Güncelleme İsteği")
public class CategoryUpdateRequest extends CategoryCreateRequest {
    @NotNull(message = "Kategori ID boş olamaz")
    @Schema(description = "Güncellenecek kategori ID'si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;
}
