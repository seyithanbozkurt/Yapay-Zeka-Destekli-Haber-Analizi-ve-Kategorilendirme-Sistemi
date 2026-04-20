package com.bitirme.repository;

import com.bitirme.entity.NewsClassificationResult;
import org.springframework.data.jpa.repository.JpaRepository;
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
}


