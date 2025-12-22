package com.bitirme.service;

import com.bitirme.dto.feed.UserFeedBackCreateRequest;
import com.bitirme.dto.feed.UserFeedBackResponse;
import com.bitirme.dto.feed.UserFeedBackUpdateRequest;

import java.util.List;

public interface UserFeedBackService {
    UserFeedBackResponse create(UserFeedBackCreateRequest request);
    UserFeedBackResponse getById(Long id);
    List<UserFeedBackResponse> getAll();
    UserFeedBackResponse update(Long id, UserFeedBackUpdateRequest request);
    void delete(Long id);
}


