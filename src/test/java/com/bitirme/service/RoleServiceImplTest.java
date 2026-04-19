package com.bitirme.service;

import com.bitirme.dto.user.RoleCreateRequest;
import com.bitirme.dto.user.RoleResponse;
import com.bitirme.dto.user.RoleUpdateRequest;
import com.bitirme.entity.Role;
import com.bitirme.exception.AlreadyExistsException;
import com.bitirme.exception.NotFoundException;
import com.bitirme.repository.RoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    @DisplayName("create")
    void createTest() {
        RoleCreateRequest req = new RoleCreateRequest();
        req.setName("MOD");
        req.setDescription("d");
        when(roleRepository.existsByName("MOD")).thenReturn(false);
        Role saved = new Role();
        saved.setId(5);
        saved.setName("MOD");
        saved.setDescription("d");
        when(roleRepository.save(any(Role.class))).thenReturn(saved);

        RoleResponse r = roleService.create(req);
        assertThat(r.getId()).isEqualTo(5);
        assertThat(r.getName()).isEqualTo("MOD");
    }

    @Test
    @DisplayName("create: isim çakışması")
    void createDuplicateTest() {
        RoleCreateRequest req = new RoleCreateRequest();
        req.setName("ADMIN");
        when(roleRepository.existsByName("ADMIN")).thenReturn(true);
        assertThrows(AlreadyExistsException.class, () -> roleService.create(req));
    }

    @Test
    @DisplayName("getById: yok")
    void getByIdNotFoundTest() {
        when(roleRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> roleService.getById(99));
    }

    @Test
    @DisplayName("delete: yok")
    void deleteNotFoundTest() {
        when(roleRepository.existsById(99)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> roleService.delete(99));
    }

    @Test
    @DisplayName("update")
    void updateTest() {
        Role existing = new Role();
        existing.setId(1);
        existing.setName("OLD");
        when(roleRepository.findById(1)).thenReturn(Optional.of(existing));
        RoleUpdateRequest upd = new RoleUpdateRequest();
        upd.setId(1);
        upd.setName("NEW");
        upd.setDescription("x");
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));

        RoleResponse out = roleService.update(1, upd);
        assertThat(out.getName()).isEqualTo("NEW");
        verify(roleRepository).save(any(Role.class));
    }
}
