package com.bitirme.dto.feed;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFeedBackCreateRequest {
    private Long newsId;
    private Long userId;
    private Integer modelVersionId;
    private Integer currentPredictedCategoryId;
    private Integer userSelectedCategoryId;
    private String feedbackType;
    private String comment;
}
