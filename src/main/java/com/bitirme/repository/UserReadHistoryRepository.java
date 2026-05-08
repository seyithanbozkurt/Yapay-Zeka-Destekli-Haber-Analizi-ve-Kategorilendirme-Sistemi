package com.bitirme.repository;

import com.bitirme.entity.UserReadHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserReadHistoryRepository extends JpaRepository<UserReadHistory, Long> {
    List<UserReadHistory> findByUserIdOrderByLastViewedAtDesc(Long userId);
    Optional<UserReadHistory> findByUserIdAndNewsId(Long userId, Long newsId);
    void deleteAllByNewsId(Long newsId);
}

