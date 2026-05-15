package com.bitirme.repository;

import com.bitirme.entity.UserSavedNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSavedNewsRepository extends JpaRepository<UserSavedNews, Long> {
    List<UserSavedNews> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<UserSavedNews> findByUserIdAndNewsId(Long userId, Long newsId);
    boolean existsByUserIdAndNewsId(Long userId, Long newsId);
    void deleteByUserIdAndNewsId(Long userId, Long newsId);
}

