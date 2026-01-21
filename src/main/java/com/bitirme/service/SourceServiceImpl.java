package com.bitirme.service;

import com.bitirme.dto.source.SourceCreateRequest;
import com.bitirme.dto.source.SourceResponse;
import com.bitirme.dto.source.SourceUpdateRequest;
import com.bitirme.entity.Source;
import com.bitirme.exception.AlreadyExistsException;
import com.bitirme.exception.NotFoundException;
import com.bitirme.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SourceServiceImpl implements SourceService {

    private final SourceRepository sourceRepository;

    @Override
    @Transactional
    public SourceResponse create(SourceCreateRequest request) {
        if (sourceRepository.existsByName(request.getName())) {
            throw new AlreadyExistsException("Haber kaynağı adı zaten kullanılıyor: " + request.getName());
        }

        Source source = new Source();
        source.setName(request.getName());
        source.setBaseUrl(request.getBaseUrl());
        source.setCategoryPath(request.getCategoryPath());
        source.setActive(request.getActive() != null ? request.getActive() : true);
        source.setCrawlUrl(request.getCrawlUrl());
        source.setTitleSelector(request.getTitleSelector());
        source.setContentSelector(request.getContentSelector());
        source.setLinkSelector(request.getLinkSelector());
        source.setCrawlType(request.getCrawlType());
        source.setLastMinuteUrl(request.getLastMinuteUrl());

        Source saved = sourceRepository.save(source);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SourceResponse getById(Integer id) {
        Source source = sourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kaynak bulunamadı: " + id));
        return toResponse(source);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SourceResponse> getAll() {
        return sourceRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SourceResponse update(Integer id, SourceUpdateRequest request) {
        Source source = sourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kaynak bulunamadı: " + id));

        if (request.getName() != null && !request.getName().equals(source.getName())) {
            if (sourceRepository.existsByName(request.getName())) {
                throw new AlreadyExistsException("Haber kaynağı adı zaten kullanılıyor: " + request.getName());
            }
            source.setName(request.getName());
        }

        if (request.getBaseUrl() != null) {
            source.setBaseUrl(request.getBaseUrl());
        }

        if (request.getCategoryPath() != null) {
            source.setCategoryPath(request.getCategoryPath());
        }

        if (request.getActive() != null) {
            source.setActive(request.getActive());
        }

        if (request.getCrawlUrl() != null) {
            source.setCrawlUrl(request.getCrawlUrl());
        }

        if (request.getTitleSelector() != null) {
            source.setTitleSelector(request.getTitleSelector());
        }

        if (request.getContentSelector() != null) {
            source.setContentSelector(request.getContentSelector());
        }

        if (request.getLinkSelector() != null) {
            source.setLinkSelector(request.getLinkSelector());
        }

        if (request.getCrawlType() != null) {
            source.setCrawlType(request.getCrawlType());
        }

        if (request.getLastMinuteUrl() != null) {
            source.setLastMinuteUrl(request.getLastMinuteUrl());
        }

        Source saved = sourceRepository.save(source);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        if (!sourceRepository.existsById(id)) {
            throw new NotFoundException("Kaynak bulunamadı: " + id);
        }
        sourceRepository.deleteById(id);
    }

    private SourceResponse toResponse(Source source) {
        SourceResponse response = new SourceResponse();
        response.setId(source.getId());
        response.setName(source.getName());
        response.setBaseUrl(source.getBaseUrl());
        response.setCategoryPath(source.getCategoryPath());
        response.setActive(source.getActive());
        response.setCrawlUrl(source.getCrawlUrl());
        response.setCrawlType(source.getCrawlType());
        response.setLastMinuteUrl(source.getLastMinuteUrl());
        return response;
    }
}


