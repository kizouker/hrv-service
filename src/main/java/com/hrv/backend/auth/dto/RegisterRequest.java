package com.hrv.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "New user registration payload")
public record RegisterRequest(
        @Schema(example = "Jane Doe")
        @NotBlank String fullName,

        @Schema(example = "jane@example.com")
        @Email @NotBlank String email,

        @Schema(example = "S3cur3P@ss!")
        @Size(min = 12, message = "Password must be at least 12 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).+$",
            message = "Password must contain uppercase, lowercase, a number, and a special character"
        )
        @NotBlank String password
) {}
