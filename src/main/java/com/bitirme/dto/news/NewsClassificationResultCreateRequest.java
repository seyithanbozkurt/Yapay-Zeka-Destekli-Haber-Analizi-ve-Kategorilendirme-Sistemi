package com.bitirme.dto.news;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class NewsClassificationResultCreateRequest {
    private Long newsId;
    private Integer modelVersionId;
    private Integer predictedCategoryId;
    private BigDecimal predictionScore;
    private Boolean active = true;
}
