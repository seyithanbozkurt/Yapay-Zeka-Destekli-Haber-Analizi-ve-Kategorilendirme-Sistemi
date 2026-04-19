package com.bitirme.service;

import com.bitirme.dto.auth.AuthResponse;
import com.bitirme.dto.auth.LoginRequest;
import com.bitirme.dto.auth.RegisterRequest;
import com.bitirme.entity.Role;
import com.bitirme.entity.User;
import com.bitirme.exception.AlreadyExistsException;
import com.bitirme.exception.NotFoundException;
import com.bitirme.exception.UnauthorizedException;
import com.bitirme.repository.RoleRepository;
import com.bitirme.repository.UserRepository;
import com.bitirme.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("login: başarılı")
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("secret");

        UserDetails ud = org.springframework.security.core.userdetails.User.withUsername("admin")
                .password("x").roles("ADMIN").build();
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(ud);
        when(jwtTokenProvider.generateToken(ud)).thenReturn("jwt-token");

        AuthResponse res = authService.login(req);

        assertThat(res.getToken()).isEqualTo("jwt-token");
        assertThat(res.getUsername()).isEqualTo("admin");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login: hatalı kimlik")
    void login_badCredentials() {
        LoginRequest req = new LoginRequest();
        req.setUsername("u");
        req.setPassword("bad");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));

        assertThrows(UnauthorizedException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("register: e-posta çakışması")
    void register_duplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("taken@test.com");
        req.setFirstName("A");
        req.setLastName("B");
        req.setBirthDate(LocalDate.of(2000, 1, 1));
        req.setPassword("pass123");
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThrows(AlreadyExistsException.class, () -> authService.register(req));
    }

    @Test
    @DisplayName("register: USER rolü yok")
    void register_userRoleMissing() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@test.com");
        req.setFirstName("A");
        req.setLastName("B");
        req.setBirthDate(LocalDate.of(2000, 1, 1));
        req.setPassword("pass123");
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authService.register(req));
    }

    @Test
    @DisplayName("register: başarılı")
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("fresh@test.com");
        req.setFirstName("A");
        req.setLastName("B");
        req.setBirthDate(LocalDate.of(2000, 1, 1));
        req.setPassword("pass123");

        Role userRole = new Role();
        userRole.setId(1);
        userRole.setName("USER");

        when(userRepository.existsByEmail("fresh@test.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.existsByUsername("fresh")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("ENC");
        UserDetails ud = org.springframework.security.core.userdetails.User.withUsername("fresh")
                .password("ENC").roles("USER").build();
        when(userDetailsService.loadUserByUsername("fresh")).thenReturn(ud);
        when(jwtTokenProvider.generateToken(ud)).thenReturn("tok");

        AuthResponse res = authService.register(req);

        assertThat(res.getToken()).isEqualTo("tok");
        verify(userRepository).save(any(User.class));
    }
}
