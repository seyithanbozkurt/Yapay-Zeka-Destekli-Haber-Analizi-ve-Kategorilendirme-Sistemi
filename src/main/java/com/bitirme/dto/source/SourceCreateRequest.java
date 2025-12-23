package com.bitirme.dto.source;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SourceCreateRequest {
    private String name;
    private String baseUrl;
    private String categoryPath;
    private Boolean active = true;
    private String crawlUrl;
    private String titleSelector;
    private String contentSelector;
    private String linkSelector;
    private String crawlType;
    private String lastMinuteUrl;
}
