package com.bitirme.service;

import com.bitirme.dto.user.SavedNewsToggleResponse;
import com.bitirme.dto.user.UserReadHistoryResponse;
import com.bitirme.dto.user.UserSavedNewsResponse;
import com.bitirme.entity.News;
import com.bitirme.entity.User;
import com.bitirme.entity.UserReadHistory;
import com.bitirme.entity.UserSavedNews;
import com.bitirme.exception.NotFoundException;
import com.bitirme.repository.NewsRepository;
import com.bitirme.repository.UserReadHistoryRepository;
import com.bitirme.repository.UserRepository;
import com.bitirme.repository.UserSavedNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserNewsActivityServiceImpl implements UserNewsActivityService {

    private final UserRepository userRepository;
    private final NewsRepository newsRepository;
    private final UserSavedNewsRepository userSavedNewsRepository;
    private final UserReadHistoryRepository userReadHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserSavedNewsResponse> getSavedNews(String username) {
        User user = findUserByUsername(username);
        return userSavedNewsRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toSavedResponse)
                .toList();
    }

    @Override
    @Transactional
    public SavedNewsToggleResponse toggleSavedNews(String username, Long newsId) {
        User user = findUserByUsername(username);
        News news = findNewsById(newsId);

        return userSavedNewsRepository.findByUserIdAndNewsId(user.getId(), newsId)
                .map(existing -> {
                    userSavedNewsRepository.delete(existing);
                    return new SavedNewsToggleResponse(false);
                })
                .orElseGet(() -> {
                    UserSavedNews savedNews = new UserSavedNews();
                    savedNews.setUser(user);
                    savedNews.setNews(news);
                    userSavedNewsRepository.save(savedNews);
                    return new SavedNewsToggleResponse(true);
                });
    }

    @Override
    @Transactional
    public void removeSavedNews(String username, Long newsId) {
        User user = findUserByUsername(username);
        userSavedNewsRepository.deleteByUserIdAndNewsId(user.getId(), newsId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSavedNews(String username, Long newsId) {
        User user = findUserByUsername(username);
        return userSavedNewsRepository.existsByUserIdAndNewsId(user.getId(), newsId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserReadHistoryResponse> getReadHistory(String username) {
        User user = findUserByUsername(username);
        return userReadHistoryRepository.findByUserIdOrderByLastViewedAtDesc(user.getId()).stream()
                .map(this::toReadHistoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public void markAsRead(String username, Long newsId) {
        User user = findUserByUsername(username);
        News news = findNewsById(newsId);

        UserReadHistory history = userReadHistoryRepository
                .findByUserIdAndNewsId(user.getId(), newsId)
                .orElseGet(() -> {
                    UserReadHistory created = new UserReadHistory();
                    created.setUser(user);
                    created.setNews(news);
                    created.setViewCount(0);
                    return created;
                });

        Integer currentViewCount = history.getViewCount();
        int safeCount = 0;
        if (currentViewCount != null) {
            safeCount = currentViewCount.intValue();
        }
        history.setViewCount(safeCount + 1);
        userReadHistoryRepository.save(history);
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı: " + username));
    }

    private News findNewsById(Long newsId) {
        return newsRepository.findById(newsId)
                .orElseThrow(() -> new NotFoundException("Haber bulunamadı: " + newsId));
    }

    private UserSavedNewsResponse toSavedResponse(UserSavedNews savedNews) {
        UserSavedNewsResponse response = new UserSavedNewsResponse();
        response.setNewsId(savedNews.getNews().getId());
        response.setTitle(savedNews.getNews().getTitle());
        response.setSourceName(savedNews.getNews().getSource() != null ? savedNews.getNews().getSource().getName() : null);
        response.setPublishedAt(savedNews.getNews().getPublishedAt());
        response.setImageUrl(savedNews.getNews().getImageUrl());
        response.setOriginalUrl(savedNews.getNews().getOriginalUrl());
        response.setSavedAt(savedNews.getCreatedAt());
        return response;
    }

    private UserReadHistoryResponse toReadHistoryResponse(UserReadHistory history) {
        UserReadHistoryResponse response = new UserReadHistoryResponse();
        response.setNewsId(history.getNews().getId());
        response.setTitle(history.getNews().getTitle());
        response.setSourceName(history.getNews().getSource() != null ? history.getNews().getSource().getName() : null);
        response.setPublishedAt(history.getNews().getPublishedAt());
        response.setImageUrl(history.getNews().getImageUrl());
        response.setOriginalUrl(history.getNews().getOriginalUrl());
        response.setViewCount(history.getViewCount());
        response.setLastViewedAt(history.getLastViewedAt());
        return response;
    }
}

