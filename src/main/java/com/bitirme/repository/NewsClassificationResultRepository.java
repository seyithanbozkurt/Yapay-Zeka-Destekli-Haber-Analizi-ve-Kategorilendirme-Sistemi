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
    
    @Query("SELECT ncr FROM NewsClassificationResult ncr WHERE ncr.news.id = :newsId AND ncr.modelVersion.id = :modelVersionId")
    Optional<NewsClassificationResult> findByNewsIdAndModelVersionId(@Param("newsId") Long newsId, @Param("modelVersionId") Integer modelVersionId);
}


