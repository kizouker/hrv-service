package com.hrv.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Change password request")
public record ChangePasswordRequest(
        @Schema(example = "OldP@ss1!")
        @NotBlank String currentPassword,

        @Schema(example = "NewP@ss123!")
        @NotBlank
        @Size(min = 12, message = "Password must be at least 12 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).+$",
            message = "Password must contain uppercase, lowercase, a number, and a special character"
        )
        String newPassword
) {}
