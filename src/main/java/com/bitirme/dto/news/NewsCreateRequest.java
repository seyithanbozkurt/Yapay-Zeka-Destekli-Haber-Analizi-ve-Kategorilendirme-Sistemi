package com.bitirme.dto.news;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class NewsCreateRequest {
    private Integer sourceId;
    private String externalId;
    private String title;
    private String content;
    private String originalUrl;
    private String language = "tr";
    private LocalDateTime publishedAt;
    private Boolean processed = false;
    private Set<Integer> categoryIds;
}
