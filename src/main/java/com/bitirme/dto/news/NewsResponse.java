package com.bitirme.dto.news;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class NewsResponse {
    private Long id;
    private Integer sourceId;
    private String sourceName;
    private String externalId;
    private String title;
    private String content;
    private String originalUrl;
    private String language;
    private LocalDateTime publishedAt;
    private LocalDateTime fetchedAt;
    private Boolean processed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<Integer> categoryIds;
    private Set<String> categoryNames;
}
