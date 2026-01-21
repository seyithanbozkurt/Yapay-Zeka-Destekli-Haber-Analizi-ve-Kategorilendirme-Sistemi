package com.bitirme.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Rol Bilgileri")
public class RoleResponse {
    @Schema(description = "Rol ID'si", example = "1")
    private Integer id;
    
    @Schema(description = "Rol adı", example = "ADMIN")
    private String name;
    
    @Schema(description = "Rol açıklaması", example = "Yönetici rolü - Tüm yetkilere sahip")
    private String description;
}
