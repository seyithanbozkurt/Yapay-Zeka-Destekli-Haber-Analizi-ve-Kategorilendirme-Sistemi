package com.bitirme.service;

import com.bitirme.dto.user.SavedNewsToggleResponse;
import com.bitirme.dto.user.UserReadHistoryResponse;
import com.bitirme.dto.user.UserSavedNewsResponse;

import java.util.List;

public interface UserNewsActivityService {
    List<UserSavedNewsResponse> getSavedNews(String username);
    SavedNewsToggleResponse toggleSavedNews(String username, Long newsId);
    void removeSavedNews(String username, Long newsId);
    boolean isSavedNews(String username, Long newsId);
    List<UserReadHistoryResponse> getReadHistory(String username);
    void markAsRead(String username, Long newsId);
}

