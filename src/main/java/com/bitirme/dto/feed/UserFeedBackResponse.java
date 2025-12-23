package com.bitirme.dto.feed;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserFeedBackResponse {
    private Long id;
    private Long newsId;
    private String newsTitle;
    private Long userId;
    private String username;
    private Integer modelVersionId;
    private String modelVersionName;
    private Integer currentPredictedCategoryId;
    private String currentPredictedCategoryName;
    private Integer userSelectedCategoryId;
    private String userSelectedCategoryName;
    private String feedbackType;
    private String comment;
    private LocalDateTime createdAt;
}
