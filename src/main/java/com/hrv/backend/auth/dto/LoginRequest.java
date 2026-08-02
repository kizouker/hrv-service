package com.hrv.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login credentials")
public record LoginRequest(
        @Schema(example = "jane@example.com")
        @Email @NotBlank String email,

        @Schema(example = "S3cur3P@ss!")
        @NotBlank String password
) {}
