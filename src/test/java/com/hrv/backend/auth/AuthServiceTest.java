package com.hrv.backend.auth;

import com.hrv.backend.auth.dto.LoginRequest;
import com.hrv.backend.auth.dto.RegisterRequest;
import com.hrv.backend.security.JwtUtil;
import com.hrv.backend.user.User;
import com.hrv.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authManager;
    @Mock JwtUtil jwtUtil;
    @InjectMocks AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).email("jane@example.com")
                .fullName("Jane Doe").password("hashed")
                .build();
    }

    @Test
    void register_success() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("S3cur3P@ss!")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(testUser);
        when(jwtUtil.generateToken(any())).thenReturn("token123");

        var response = authService.register(
                new RegisterRequest("Jane Doe", "jane@example.com", "S3cur3P@ss!"));

        assertThat(response.token()).isEqualTo("token123");
        assertThat(response.email()).isEqualTo("jane@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Jane Doe", "jane@example.com", "pass")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void login_success() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUser)).thenReturn("jwt-token");

        var response = authService.login(new LoginRequest("jane@example.com", "S3cur3P@ss!"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.fullName()).isEqualTo("Jane Doe");
    }

    @Test
    void login_badCredentials_throws() {
        doThrow(new BadCredentialsException("bad"))
                .when(authManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("jane@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
