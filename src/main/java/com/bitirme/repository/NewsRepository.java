package com.bitirme.repository;

import com.bitirme.entity.News;
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
    List<News> findBySourceId(Integer sourceId);
    List<News> findByProcessedFalse();
    List<News> findByPublishedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT n FROM News n WHERE n.source.id = :sourceId AND n.publishedAt >= :fromDate")
    List<News> findBySourceIdAndPublishedAfter(@Param("sourceId") Integer sourceId, @Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Normalize edilmiş title'a göre benzer haberleri bulur.
     * Java tarafında normalize edilmiş title ile veritabanındaki title'ları karşılaştırır.
     * PostgreSQL native query kullanarak REGEXP_REPLACE ile özel karakterleri temizler.
     */
    @Query(value = "SELECT * FROM news WHERE LOWER(REGEXP_REPLACE(title, '[^a-zA-ZçğıöşüÇĞIİÖŞÜ0-9\\s]', '', 'g')) = LOWER(:normalizedTitle)", nativeQuery = true)
    List<News> findByNormalizedTitle(@Param("normalizedTitle") String normalizedTitle);
}


