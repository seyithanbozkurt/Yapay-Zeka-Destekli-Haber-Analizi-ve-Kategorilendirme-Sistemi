package com.bitirme.service;

import com.bitirme.dto.user.RoleCreateRequest;
import com.bitirme.dto.user.RoleResponse;
import com.bitirme.dto.user.RoleUpdateRequest;

import java.util.List;

public interface RoleService {
    RoleResponse create(RoleCreateRequest request);
    RoleResponse getById(Integer id);
    List<RoleResponse> getAll();
    RoleResponse update(Integer id, RoleUpdateRequest request);
    void delete(Integer id);
}


