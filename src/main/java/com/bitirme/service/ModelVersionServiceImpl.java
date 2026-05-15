package com.bitirme.service;

import com.bitirme.dto.model.ModelVersionCreateRequest;
import com.bitirme.dto.model.ModelVersionResponse;
import com.bitirme.dto.model.ModelVersionUpdateRequest;
import com.bitirme.entity.ModelVersion;
import com.bitirme.entity.User;
import com.bitirme.exception.NotFoundException;
import com.bitirme.repository.ModelVersionRepository;
import com.bitirme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelVersionServiceImpl implements ModelVersionService {

    private final ModelVersionRepository modelVersionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    @CacheEvict(value = {"model_versions_all", "model_version_by_id"}, allEntries = true)
    public ModelVersionResponse create(ModelVersionCreateRequest request) {
        ModelVersion modelVersion = new ModelVersion();
        modelVersion.setName(request.getName());
        modelVersion.setDescription(request.getDescription());

        if (request.getCreatedById() != null) {
            User user = userRepository.findById(request.getCreatedById())
                    .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı: " + request.getCreatedById()));
            modelVersion.setCreatedBy(user);
        }

        ModelVersion saved = modelVersionRepository.save(modelVersion);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "model_version_by_id", key = "#id")
    public ModelVersionResponse getById(Integer id) {
        ModelVersion modelVersion = modelVersionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Model versiyonu bulunamadı: " + id));
        return toResponse(modelVersion);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("model_versions_all")
    public List<ModelVersionResponse> getAll() {
        return modelVersionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"model_versions_all", "model_version_by_id"}, allEntries = true)
    public ModelVersionResponse update(Integer id, ModelVersionUpdateRequest request) {
        ModelVersion modelVersion = modelVersionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Model versiyonu bulunamadı: " + id));

        if (request.getName() != null) {
            modelVersion.setName(request.getName());
        }

        if (request.getDescription() != null) {
            modelVersion.setDescription(request.getDescription());
        }

        ModelVersion saved = modelVersionRepository.save(modelVersion);
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"model_versions_all", "model_version_by_id"}, allEntries = true)
    public void delete(Integer id) {
        if (!modelVersionRepository.existsById(id)) {
            throw new NotFoundException("Model versiyonu bulunamadı: " + id);
        }
        modelVersionRepository.deleteById(id);
    }

    private ModelVersionResponse toResponse(ModelVersion modelVersion) {
        ModelVersionResponse response = new ModelVersionResponse();
        response.setId(modelVersion.getId());
        response.setName(modelVersion.getName());
        response.setDescription(modelVersion.getDescription());
        if (modelVersion.getCreatedBy() != null) {
            response.setCreatedByUsername(modelVersion.getCreatedBy().getUsername());
        }
        return response;
    }
}


