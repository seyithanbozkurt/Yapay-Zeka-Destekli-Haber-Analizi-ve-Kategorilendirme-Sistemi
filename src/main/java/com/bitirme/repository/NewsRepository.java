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
    
    List<News> findBySourceIdAndPublishedAtGreaterThanEqual(Integer sourceId, LocalDateTime fromDate);
    
    /** Normalize edilmiş başlığa göre haberleri bulur (normalized_title kolonu kullanılır). */
    List<News> findByNormalizedTitle(String normalizedTitle);

    /** normalized_title boş olan haberler (backfill için). */
    List<News> findByNormalizedTitleIsNull();

    /** image_url boş olan ve URL'si bulunan haberler (görsel backfill için). */
    @Query("SELECT n FROM News n WHERE (n.imageUrl IS NULL OR n.imageUrl = '') AND n.originalUrl IS NOT NULL AND n.originalUrl <> ''")
    List<News> findMissingImageUrlNews();

    /**
     * PostgreSQL: title/content veritabanında hâlâ {@code bytea} ise JPQL {@code LOWER(...)} patlar.
     * {@code pg_typeof} ile metin araması hem {@code text} hem {@code bytea} kolonlarda çalışır.
     */
    @Query(
            value = """
                    SELECT DISTINCT n.* FROM news n
                    LEFT JOIN sources s ON s.id = n.source_id
                    LEFT JOIN news_categories nc ON n.id = nc.news_id
                    LEFT JOIN categories c ON c.id = nc.category_id
                    WHERE (:search IS NULL
                        OR lower(CASE WHEN pg_typeof(n.title) = 'bytea'::regtype
                                 THEN convert_from(n.title::bytea, 'UTF8'::name) ELSE n.title::text END)
                           LIKE lower(concat('%', :search, '%'))
                        OR lower(CASE WHEN pg_typeof(n.content) = 'bytea'::regtype
                                 THEN convert_from(n.content::bytea, 'UTF8'::name) ELSE n.content::text END)
                           LIKE lower(concat('%', :search, '%')))
                    AND (:sourceName IS NULL OR s.name = :sourceName)
                    AND (:categoryName IS NULL OR c.name = :categoryName)
                    ORDER BY n.published_at DESC NULLS LAST, n.id DESC
                    """,
            countQuery = """
                    SELECT count(DISTINCT n.id) FROM news n
                    LEFT JOIN sources s ON s.id = n.source_id
                    LEFT JOIN news_categories nc ON n.id = nc.news_id
                    LEFT JOIN categories c ON c.id = nc.category_id
                    WHERE (:search IS NULL
                        OR lower(CASE WHEN pg_typeof(n.title) = 'bytea'::regtype
                                 THEN convert_from(n.title::bytea, 'UTF8'::name) ELSE n.title::text END)
                           LIKE lower(concat('%', :search, '%'))
                        OR lower(CASE WHEN pg_typeof(n.content) = 'bytea'::regtype
                                 THEN convert_from(n.content::bytea, 'UTF8'::name) ELSE n.content::text END)
                           LIKE lower(concat('%', :search, '%')))
                    AND (:sourceName IS NULL OR s.name = :sourceName)
                    AND (:categoryName IS NULL OR c.name = :categoryName)
                    """,
            nativeQuery = true
    )
    Page<News> findPageWithFilters(
            @Param("search") String search,
            @Param("sourceName") String sourceName,
            @Param("categoryName") String categoryName,
            Pageable pageable
    );
}


