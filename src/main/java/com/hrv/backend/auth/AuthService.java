package com.hrv.backend.auth;

import com.hrv.backend.auth.dto.*;
import com.hrv.backend.security.JwtUtil;
import com.hrv.backend.user.User;
import com.hrv.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = User.builder()
                .fullName(req.fullName())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .build();
        userRepository.save(user);
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getFullName());
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        User user = userRepository.findByEmail(req.email()).orElseThrow();
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getFullName());
    }
}
