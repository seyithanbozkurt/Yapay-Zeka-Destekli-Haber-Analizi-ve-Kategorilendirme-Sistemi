package com.bitirme.dto.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ModelVersionResponse {
    private Integer id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private Long createdById;
    private String createdByUsername;
}
