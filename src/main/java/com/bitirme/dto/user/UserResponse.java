package com.bitirme.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Schema(description = "Kullanıcı Bilgileri")
public class UserResponse {
    @Schema(description = "Kullanıcı ID'si", example = "1")
    private Long id;
    
    @Schema(description = "Kullanıcı adı", example = "metehan")
    private String username;
    
    @Schema(description = "E-posta adresi", example = "metehan@bitirme.com")
    private String email;
    
    @Schema(description = "Aktiflik durumu", example = "true")
    private Boolean active;
    
    @Schema(description = "Kullanıcı rolleri", example = "[\"ADMIN\", \"USER\"]")
    private Set<String> roles;
}
