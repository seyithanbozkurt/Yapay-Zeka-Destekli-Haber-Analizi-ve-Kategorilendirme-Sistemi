package com.bitirme.dto.category;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryCreateRequest {
    private String name;
    private String description;
    private Boolean active = true;
}
