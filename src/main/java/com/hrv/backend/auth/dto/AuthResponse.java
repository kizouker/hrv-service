package com.hrv.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT token response")
public record AuthResponse(
        @Schema(example = "eyJhbGci...") String token,
        @Schema(example = "jane@example.com") String email,
        @Schema(example = "Jane Doe") String fullName
) {}
