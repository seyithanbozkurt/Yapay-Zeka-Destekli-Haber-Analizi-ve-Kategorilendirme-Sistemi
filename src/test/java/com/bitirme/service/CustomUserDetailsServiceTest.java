package com.bitirme.service;

import com.bitirme.entity.Role;
import com.bitirme.entity.User;
import com.bitirme.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("loadUserByUsername: başarılı")
    void loadUserByUsername_success() {
        Role r = new Role();
        r.setName("ADMIN");
        User u = new User();
        u.setUsername("admin");
        u.setPasswordHash("{bcrypt}hash");
        u.setActive(true);
        u.setRoles(Set.of(r));
        when(userRepository.findByUsername("admin")).thenReturn(java.util.Optional.of(u));

        UserDetails ud = customUserDetailsService.loadUserByUsername("admin");

        assertThat(ud.getUsername()).isEqualTo("admin");
        assertThat(ud.getAuthorities()).extracting(Object::toString).anyMatch(a -> a.contains("ADMIN"));
    }

    @Test
    @DisplayName("loadUserByUsername: kullanıcı yok")
    void loadUserByUsername_missing() {
        when(userRepository.findByUsername("x")).thenReturn(java.util.Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> customUserDetailsService.loadUserByUsername("x"));
    }

    @Test
    @DisplayName("loadUserByUsername: pasif kullanıcı")
    void loadUserByUsername_inactive() {
        User u = new User();
        u.setUsername("u");
        u.setPasswordHash("h");
        u.setActive(false);
        when(userRepository.findByUsername("u")).thenReturn(java.util.Optional.of(u));

        assertThrows(UsernameNotFoundException.class, () -> customUserDetailsService.loadUserByUsername("u"));
    }
}
