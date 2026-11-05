package com.hrv.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET =
            "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhNQUMtU0hBLTI1Ng==";

    private JwtUtil jwtUtil;
    private UserDetails user;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
        user = new User("jane@example.com", "password", List.of());
    }

    @Test
    void generateAndExtract_roundtrip() {
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("jane@example.com");
    }

    @Test
    void isTokenValid_freshToken_returnsTrue() {
        assertThat(jwtUtil.isTokenValid(jwtUtil.generateToken(user), user)).isTrue();
    }

    @Test
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken(user) + "tampered";
        assertThat(jwtUtil.isTokenValid(token, user)).isFalse();
    }

    @Test
    void isTokenValid_wrongUser_returnsFalse() {
        UserDetails other = new User("other@example.com", "password", List.of());
        assertThat(jwtUtil.isTokenValid(jwtUtil.generateToken(user), other)).isFalse();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1000L);
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.isTokenValid(token, user)).isFalse();
    }
}
