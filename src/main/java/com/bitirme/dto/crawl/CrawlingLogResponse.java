package com.bitirme.dto.crawl;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Haber Çekme Log Bilgileri")
public class CrawlingLogResponse {
    
    @Schema(description = "Haber kaynağı adı", example = "Hürriyet")
    private String sourceName;
    
    @Schema(description = "Başlangıç zamanı", example = "2024-01-21T10:00:00")
    private LocalDateTime startedAt;
    
    @Schema(description = "Bitiş zamanı", example = "2024-01-21T10:05:00")
    private LocalDateTime finishedAt;
    
    @Schema(description = "Durum", example = "SUCCESS")
    private String status;
    
    @Schema(description = "Çekilen haber sayısı", example = "25")
    private Integer fetchedCount;
    
    @Schema(description = "Hata mesajı (varsa)", example = "Bağlantı hatası")
    private String errorMessage;
}


