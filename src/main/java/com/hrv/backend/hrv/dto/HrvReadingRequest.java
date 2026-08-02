package com.hrv.backend.hrv.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

@Schema(description = "Submit a new HRV reading")
public record HrvReadingRequest(
        @Schema(description = "Device ID (omit for manual entry)") Long deviceId,
        @Schema(example = "2024-06-01T07:30:00Z") @NotNull Instant measuredAt,
        @Schema(example = "55.3", description = "RMSSD in ms") @Positive Double rmssd,
        @Schema(example = "48.1", description = "SDNN in ms")  @Positive Double sdnn,
        @Schema(example = "22.5", description = "pNN50 %")     @Positive Double pnn50,
        @Schema(example = "820.0", description = "Mean RR in ms") @Positive Double meanRr,
        @Schema(example = "73.0", description = "Mean HR in bpm") @Positive Double meanHr,
        @Schema(example = "612.0", description = "LF power ms²") Double lfPower,
        @Schema(example = "1024.0", description = "HF power ms²") Double hfPower,
        @Schema(example = "0.60", description = "LF/HF ratio") Double lfHfRatio,
        @Schema(example = "Morning, after coffee") String notes
) {}
