package com.bitirme.dto.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModelVersionCreateRequest {
    private String name;
    private String description;
    private Long createdById;
}
