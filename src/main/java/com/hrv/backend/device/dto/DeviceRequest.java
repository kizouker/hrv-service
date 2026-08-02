package com.hrv.backend.device.dto;

import com.hrv.backend.device.Device.DeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceRequest(
        @Schema(example = "My Polar H10") @NotBlank String name,
        @Schema(example = "POLAR") @NotNull DeviceType type,
        @Schema(example = "PH10-ABC123") String externalDeviceId
) {}
