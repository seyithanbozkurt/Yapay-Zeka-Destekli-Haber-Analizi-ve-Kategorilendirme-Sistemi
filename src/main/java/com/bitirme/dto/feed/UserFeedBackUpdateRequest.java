package com.bitirme.dto.feed;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFeedBackUpdateRequest {
    private Integer modelVersionId;
    private Integer currentPredictedCategoryId;
    private Integer userSelectedCategoryId;
    private String feedbackType;
    private String comment;
}
