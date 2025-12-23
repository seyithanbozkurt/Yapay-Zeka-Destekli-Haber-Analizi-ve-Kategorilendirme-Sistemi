package com.bitirme.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpdateRequest {
    private String name;
    private String description;
}
