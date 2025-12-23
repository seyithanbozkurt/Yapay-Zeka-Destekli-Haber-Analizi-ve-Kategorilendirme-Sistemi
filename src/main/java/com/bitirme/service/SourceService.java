package com.bitirme.service;

import com.bitirme.dto.source.SourceCreateRequest;
import com.bitirme.dto.source.SourceResponse;
import com.bitirme.dto.source.SourceUpdateRequest;

import java.util.List;

public interface SourceService {
    SourceResponse create(SourceCreateRequest request);
    SourceResponse getById(Integer id);
    List<SourceResponse> getAll();
    SourceResponse update(Integer id, SourceUpdateRequest request);
    void delete(Integer id);
}


