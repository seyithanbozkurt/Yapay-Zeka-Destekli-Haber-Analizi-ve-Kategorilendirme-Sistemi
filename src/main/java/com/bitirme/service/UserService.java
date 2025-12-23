package com.bitirme.service;

import com.bitirme.dto.user.UserCreateRequest;
import com.bitirme.dto.user.UserResponse;
import com.bitirme.dto.user.UserUpdateRequest;

import java.util.List;

public interface UserService {
    UserResponse create(UserCreateRequest request);
    UserResponse getById(Long id);
    List<UserResponse> getAll();
    UserResponse update(Long id, UserUpdateRequest request);
    void delete(Long id);
}


