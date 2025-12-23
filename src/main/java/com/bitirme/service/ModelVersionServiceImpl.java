package com.bitirme.service;

import com.bitirme.dto.model.ModelVersionCreateRequest;
import com.bitirme.dto.model.ModelVersionResponse;
import com.bitirme.dto.model.ModelVersionUpdateRequest;
import com.bitirme.entity.ModelVersion;
import com.bitirme.entity.User;
import com.bitirme.repository.ModelVersionRepository;
import com.bitirme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModelVersionServiceImpl implements ModelVersionService {

    private final ModelVersionRepository modelVersionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ModelVersionResponse create(ModelVersionCreateRequest request) {
        ModelVersion modelVersion = new ModelVersion();
        modelVersion.setName(request.getName());
        modelVersion.setDescription(request.getDescription());

        if (request.getCreatedById() != null) {
            User user = userRepository.findById(request.getCreatedById())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getCreatedById()));
            modelVersion.setCreatedBy(user);
        }

        ModelVersion saved = modelVersionRepository.save(modelVersion);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ModelVersionResponse getById(Integer id) {
        ModelVersion modelVersion = modelVersionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ModelVersion not found with id: " + id));
        return toResponse(modelVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModelVersionResponse> getAll() {
        return modelVersionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ModelVersionResponse update(Integer id, ModelVersionUpdateRequest request) {
        ModelVersion modelVersion = modelVersionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ModelVersion not found with id: " + id));

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
    public void delete(Integer id) {
        if (!modelVersionRepository.existsById(id)) {
            throw new RuntimeException("ModelVersion not found with id: " + id);
        }
        modelVersionRepository.deleteById(id);
    }

    private ModelVersionResponse toResponse(ModelVersion modelVersion) {
        ModelVersionResponse response = new ModelVersionResponse();
        response.setId(modelVersion.getId());
        response.setName(modelVersion.getName());
        response.setDescription(modelVersion.getDescription());
        response.setCreatedAt(modelVersion.getCreatedAt());
        if (modelVersion.getCreatedBy() != null) {
            response.setCreatedById(modelVersion.getCreatedBy().getId());
            response.setCreatedByUsername(modelVersion.getCreatedBy().getUsername());
        }
        return response;
    }
}


