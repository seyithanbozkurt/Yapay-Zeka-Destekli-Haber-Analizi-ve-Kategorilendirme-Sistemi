package com.bitirme.dto.user;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserSavedNewsResponse {
    private Long newsId;
    private String title;
    private String sourceName;
    private LocalDateTime publishedAt;
    private String imageUrl;
    private String originalUrl;
    private LocalDateTime savedAt;
}

