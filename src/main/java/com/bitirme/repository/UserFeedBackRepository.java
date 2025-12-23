package com.bitirme.repository;

import com.bitirme.entity.UserFeedBack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFeedBackRepository extends JpaRepository<UserFeedBack, Long> {
    List<UserFeedBack> findByUserId(Long userId);
    List<UserFeedBack> findByNewsId(Long newsId);
    List<UserFeedBack> findByModelVersionId(Integer modelVersionId);
    List<UserFeedBack> findByUserSelectedCategoryId(Integer categoryId);
}


