package com.bitirme.dto.crawl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlingLogResponse {

    private Long id;
    private Integer sourceId;
    private String sourceName;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String status;
    private Integer fetchedCount;
    private String errorMessage;
}


