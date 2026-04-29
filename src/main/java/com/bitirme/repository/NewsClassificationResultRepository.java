package com.bitirme.repository;

import com.bitirme.entity.NewsClassificationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsClassificationResultRepository extends JpaRepository<NewsClassificationResult, Long> {
    List<NewsClassificationResult> findByNewsId(Long newsId);
    List<NewsClassificationResult> findByModelVersionId(Integer modelVersionId);
    List<NewsClassificationResult> findByPredictedCategoryId(Integer categoryId);
    List<NewsClassificationResult> findByActiveTrue();

    Optional<NewsClassificationResult> findByNewsIdAndModelVersionId(Long newsId, Integer modelVersionId);

    /**
     * Verilen kategori için aktif ({@code is_active = true}) sınıflandırma satırı sayısı.
     * Not: Aynı habere ait birden fazla aktif satır varsa hepsi sayılır (tohum mantığı satır bazlıdır).
     */
    @Query("SELECT COUNT(r) FROM NewsClassificationResult r WHERE r.predictedCategory.id = :categoryId AND r.active = true")
    long countByPredictedCategoryIdAndActiveTrue(@Param("categoryId") Integer categoryId);

    /** Aktif sonuçları olan tekil haber sayısı (aynı habere birden fazla aktif sonuç olsa bir kez sayılır). */
    @Query("SELECT COUNT(DISTINCT r.news.id) FROM NewsClassificationResult r WHERE r.active = true")
    long countDistinctNewsWithActiveClassification();
}
