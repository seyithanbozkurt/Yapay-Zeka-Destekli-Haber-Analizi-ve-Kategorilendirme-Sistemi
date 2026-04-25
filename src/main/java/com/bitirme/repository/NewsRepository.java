package com.bitirme.repository;

import com.bitirme.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    Optional<News> findByExternalIdAndSourceId(String externalId, Integer sourceId);

    /** Aynı kaynaktan aynı normalize başlığa sahip haber var mı (Takvim vb. aynı haber farklı URL tekrarını engeller). */
    boolean existsBySourceIdAndNormalizedTitle(Integer sourceId, String normalizedTitle);

    List<News> findBySourceId(Integer sourceId);
    List<News> findByProcessedFalse();
    List<News> findByPublishedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT n FROM News n WHERE n.source.id = :sourceId AND n.publishedAt >= :fromDate")
    List<News> findBySourceIdAndPublishedAfter(@Param("sourceId") Integer sourceId, @Param("fromDate") LocalDateTime fromDate);
    
    /** Normalize edilmiş başlığa göre haberleri bulur (normalized_title kolonu kullanılır). */
    List<News> findByNormalizedTitle(String normalizedTitle);

    /** normalized_title boş olan haberler (backfill için). */
    List<News> findByNormalizedTitleIsNull();

    /** image_url boş olan ve URL'si bulunan haberler (görsel backfill için). */
    @Query("SELECT n FROM News n WHERE (n.imageUrl IS NULL OR n.imageUrl = '') AND n.originalUrl IS NOT NULL AND n.originalUrl <> ''")
    List<News> findMissingImageUrlNews();

    @Query(
            value = "SELECT DISTINCT n FROM News n " +
                    "LEFT JOIN n.source s " +
                    "LEFT JOIN n.categories c " +
                    "WHERE (:search IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:sourceName IS NULL OR s.name = :sourceName) " +
                    "AND (:categoryName IS NULL OR c.name = :categoryName)",
            countQuery = "SELECT COUNT(DISTINCT n.id) FROM News n " +
                    "LEFT JOIN n.source s " +
                    "LEFT JOIN n.categories c " +
                    "WHERE (:search IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:sourceName IS NULL OR s.name = :sourceName) " +
                    "AND (:categoryName IS NULL OR c.name = :categoryName)"
    )
    Page<News> findPageWithFilters(
            @Param("search") String search,
            @Param("sourceName") String sourceName,
            @Param("categoryName") String categoryName,
            Pageable pageable
    );
}


