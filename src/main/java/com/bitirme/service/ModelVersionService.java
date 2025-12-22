package com.bitirme.service;

import com.bitirme.dto.model.ModelVersionCreateRequest;
import com.bitirme.dto.model.ModelVersionResponse;
import com.bitirme.dto.model.ModelVersionUpdateRequest;

import java.util.List;

public interface ModelVersionService {
    ModelVersionResponse create(ModelVersionCreateRequest request);
    ModelVersionResponse getById(Integer id);
    List<ModelVersionResponse> getAll();
    ModelVersionResponse update(Integer id, ModelVersionUpdateRequest request);
    void delete(Integer id);
}


