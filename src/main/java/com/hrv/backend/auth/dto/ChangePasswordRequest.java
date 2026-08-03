package com.hrv.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Change password request")
public record ChangePasswordRequest(
        @Schema(example = "OldP@ss1")
        @NotBlank String currentPassword,

        @Schema(example = "NewP@ss123!")
        @NotBlank @Size(min = 8) String newPassword
) {}
