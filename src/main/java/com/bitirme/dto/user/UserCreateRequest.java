package com.bitirme.dto.user;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserCreateRequest {
    private String username;
    private String email;
    private String passwordHash;
    private Boolean active = true;
    private Set<Integer> roleIds;
}
