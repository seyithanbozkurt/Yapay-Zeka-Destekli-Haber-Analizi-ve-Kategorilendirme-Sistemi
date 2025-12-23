package com.bitirme.repository;

import com.bitirme.entity.CrawlingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrawlingLogRepository extends JpaRepository<CrawlingLog, Long> {
    List<CrawlingLog> findBySourceId(Integer sourceId);
    List<CrawlingLog> findByStatus(String status);
}


