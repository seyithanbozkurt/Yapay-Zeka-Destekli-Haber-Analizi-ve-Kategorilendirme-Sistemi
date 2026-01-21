package com.bitirme.service;

import com.bitirme.dto.user.RoleCreateRequest;
import com.bitirme.dto.user.RoleResponse;
import com.bitirme.dto.user.RoleUpdateRequest;
import com.bitirme.entity.Role;
import com.bitirme.exception.AlreadyExistsException;
import com.bitirme.exception.NotFoundException;
import com.bitirme.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public RoleResponse create(RoleCreateRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new AlreadyExistsException("Rol adı zaten kullanılıyor: " + request.getName());
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());

        Role saved = roleRepository.save(role);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getById(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rol bulunamadı: " + id));
        return toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAll() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RoleResponse update(Integer id, RoleUpdateRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rol bulunamadı: " + id));

        if (request.getName() != null && !request.getName().equals(role.getName())) {
            if (roleRepository.existsByName(request.getName())) {
                throw new AlreadyExistsException("Rol adı zaten kullanılıyor: " + request.getName());
            }
            role.setName(request.getName());
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        Role saved = roleRepository.save(role);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        if (!roleRepository.existsById(id)) {
            throw new NotFoundException("Rol bulunamadı: " + id);
        }
        roleRepository.deleteById(id);
    }

    private RoleResponse toResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setName(role.getName());
        response.setDescription(role.getDescription());
        return response;
    }
}


