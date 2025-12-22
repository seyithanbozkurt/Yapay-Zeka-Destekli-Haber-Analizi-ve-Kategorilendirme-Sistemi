package com.bitirme.dto.news;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class NewsClassificationResultResponse {
    private Long id;
    private Long newsId;
    private String newsTitle;
    private Integer modelVersionId;
    private String modelVersionName;
    private Integer predictedCategoryId;
    private String predictedCategoryName;
    private BigDecimal predictionScore;
    private LocalDateTime classifiedAt;
    private Boolean active;
}
