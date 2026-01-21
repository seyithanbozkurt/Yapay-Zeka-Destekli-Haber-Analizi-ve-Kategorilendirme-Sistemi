package com.bitirme.dto.feed;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Kullanıcı Geri Bildirimi Güncelleme İsteği")
public class UserFeedBackUpdateRequest extends UserFeedBackCreateRequest {
    @NotNull(message = "Geri bildirim ID boş olamaz")
    @Schema(description = "Güncellenecek geri bildirim ID'si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
