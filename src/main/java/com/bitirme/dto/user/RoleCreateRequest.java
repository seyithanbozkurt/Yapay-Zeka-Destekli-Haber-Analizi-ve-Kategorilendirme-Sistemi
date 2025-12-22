package com.bitirme.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleCreateRequest {
    private String name;
    private String description;
}
