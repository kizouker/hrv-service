package com.hrv.backend.hrv.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Batch sync payload from iOS / watchOS HealthKit.
 *
 * The iOS app converts HealthKit HKHeartRateVariabilityMetadataKeyAlgorithm
 * samples and sends them here. Duplicate measuredAt timestamps are silently
 * skipped (idempotent sync).
 */
@Schema(description = "Batch upload of HRV readings from iOS/watchOS HealthKit")
public record HealthKitSyncRequest(

        @Schema(description = "Device/source name (e.g. 'Apple Watch Series 9')")
        String sourceName,

        @Schema(description = "Apple device UUID from UIDevice.current.identifierForVendor")
        String appleDeviceId,

        @Schema(description = "List of HRV samples to sync — duplicates are skipped")
        @NotEmpty @Valid List<HrvReadingRequest> readings
) {}
