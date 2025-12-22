package com.bitirme.dto.news;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class NewsUpdateRequest {
    private Integer sourceId;
    private String externalId;
    private String title;
    private String content;
    private String originalUrl;
    private String language;
    private LocalDateTime publishedAt;
    private Boolean processed;
    private Set<Integer> categoryIds;
}
