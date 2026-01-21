package com.bitirme.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Kullanıcı Güncelleme İsteği")
public class UserUpdateRequest extends UserCreateRequest {
    @NotNull(message = "Kullanıcı ID boş olamaz")
    @Schema(description = "Güncellenecek kullanıcı ID'si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
