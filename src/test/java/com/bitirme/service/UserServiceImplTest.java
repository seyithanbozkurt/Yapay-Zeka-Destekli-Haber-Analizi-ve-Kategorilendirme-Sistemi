package com.bitirme.service;

import com.bitirme.dto.user.UserCreateRequest;
import com.bitirme.dto.user.UserResponse;
import com.bitirme.entity.Role;
import com.bitirme.entity.User;
import com.bitirme.exception.AlreadyExistsException;
import com.bitirme.exception.NotFoundException;
import com.bitirme.repository.RoleRepository;
import com.bitirme.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(2);
        userRole.setName("USER");
    }

    @Test
    @DisplayName("create: başarılı kayıt")
    void createSuccessTest() {
        UserCreateRequest req = new UserCreateRequest();
        req.setUsername("u1");
        req.setEmail("u1@test.com");
        req.setPasswordHash("hash");
        req.setActive(true);
        req.setRoleIds(Set.of(2));

        when(userRepository.existsByUsername("u1")).thenReturn(false);
        when(userRepository.existsByEmail("u1@test.com")).thenReturn(false);
        when(roleRepository.findById(2)).thenReturn(Optional.of(userRole));

        User saved = new User();
        saved.setId(10L);
        saved.setUsername("u1");
        saved.setEmail("u1@test.com");
        saved.setPasswordHash("hash");
        saved.setActive(true);
        saved.setRoles(Set.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        UserResponse res = userService.create(req);

        assertThat(res.getId()).isEqualTo(10L);
        assertThat(res.getUsername()).isEqualTo("u1");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("create: kullanıcı adı mevcut")
    void createDuplicateUsernameTest() {
        UserCreateRequest req = new UserCreateRequest();
        req.setUsername("dup");
        req.setEmail("e@test.com");
        req.setPasswordHash("h");
        when(userRepository.existsByUsername("dup")).thenReturn(true);

        assertThrows(AlreadyExistsException.class, () -> userService.create(req));
    }

    @Test
    @DisplayName("getById: bulunamadı")
    void getByIdNotFoundTest() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.getById(99L));
    }

    @Test
    @DisplayName("delete: bulunamadı")
    void deleteNotFoundTest() {
        when(userRepository.existsById(99L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> userService.delete(99L));
    }
}
