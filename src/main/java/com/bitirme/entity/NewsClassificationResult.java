package com.bitirme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "news_classification_results")
@Getter
@Setter
public class NewsClassificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_version_id", nullable = false)
    private ModelVersion modelVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predicted_category_id", nullable = false)
    private Category predictedCategory;

    @Column(name = "prediction_score", precision = 5, scale = 4)
    private BigDecimal predictionScore;

    @CreationTimestamp
    @Column(name = "classified_at", nullable = false, updatable = false)
    private LocalDateTime classifiedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;
}
