package com.bitirme.dto.category;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryResponse {
    private Integer id;
    private String name;
    private String description;
    private Boolean active;
}
