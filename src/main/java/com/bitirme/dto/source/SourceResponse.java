package com.bitirme.dto.source;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SourceResponse {
    private Integer id;
    private String name;
    private String baseUrl;
    private String categoryPath;
    private Boolean active;
    private LocalDateTime createdAt;
    private String crawlUrl;
    private String titleSelector;
    private String contentSelector;
    private String linkSelector;
    private String crawlType;
    private String lastMinuteUrl;
}
