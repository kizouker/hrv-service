package com.hrv.backend.hrv;

import com.hrv.backend.hrv.dto.HealthKitSyncRequest;
import com.hrv.backend.hrv.dto.HrvReadingResponse;
import com.hrv.backend.hrv.dto.SyncResponse;
import com.hrv.backend.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Tag(name = "iOS / watchOS Sync", description = "Batch upload from HealthKit — idempotent, duplicate-safe")
@SecurityRequirement(name = "bearerAuth")
public class HrvSyncController {

    private final HrvRepository hrvRepository;
    private final HrvService hrvService;

    /**
     * POST /api/sync/healthkit
     *
     * iOS app sends all locally stored readings since last sync.
     * Already-synced timestamps are detected and skipped, so it's safe
     * to call on every app foreground / background refresh.
     *
     * Swift usage:
     *   let body = HealthKitSyncRequest(sourceName: "Apple Watch", readings: samples)
     *   URLSession.shared.dataTask(with: request)...
     */
    @PostMapping("/healthkit")
    @Operation(
        summary = "Batch sync from iOS / watchOS HealthKit",
        description = """
            Send all pending HRV readings from the device in one call.
            Already-synced readings (matched by measuredAt timestamp) are skipped — safe to call repeatedly.
            
            **iOS integration steps:**
            1. Request HealthKit authorisation for `HKQuantityTypeIdentifierHeartRateVariabilitySDNN`
            2. Query samples since last sync date (store in UserDefaults)
            3. Map `HKQuantitySample` → `HrvReadingRequest` (rmssd from SDNN if RMSSD unavailable)
            4. POST to this endpoint with your JWT in the Authorization header
            5. Update last-sync date on success
            """
    )
    public ResponseEntity<SyncResponse> syncHealthKit(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody HealthKitSyncRequest req) {

        // Collect timestamps already in DB to skip duplicates.
        // Truncate both sides: sub-millisecond precision does not survive a
        // DB round-trip, so raw Instants would never match re-posted payloads.
        List<HrvReading> existing = hrvRepository.findAllByUserIdOrderByMeasuredAtDesc(user.getId());
        Set<Instant> existingTimestamps = existing.stream()
                .map(r -> r.getMeasuredAt().truncatedTo(ChronoUnit.MILLIS))
                .collect(Collectors.toSet());

        List<HrvReadingResponse> saved = new ArrayList<>();
        int skipped = 0;

        for (var reading : req.readings()) {
            Instant key = reading.measuredAt().truncatedTo(ChronoUnit.MILLIS);
            if (existingTimestamps.contains(key)) {
                skipped++;
            } else {
                saved.add(hrvService.create(user, reading));
                existingTimestamps.add(key); // prevent intra-batch dupes
            }
        }

        return ResponseEntity.ok(new SyncResponse(req.readings().size(), saved.size(), skipped));
    }

    /**
     * GET /api/sync/status
     * Returns the timestamp of the most recent reading — iOS uses this
     * as the anchor date for the next HealthKit query.
     */
    @GetMapping("/status")
    @Operation(summary = "Last sync timestamp — use as HealthKit anchor date on iOS")
    public ResponseEntity<?> status(@AuthenticationPrincipal User user) {
        return hrvRepository.findAllByUserIdOrderByMeasuredAtDesc(user.getId())
                .stream().findFirst()
                .map(r -> ResponseEntity.ok(new LastSyncResponse(r.getMeasuredAt(), true)))
                .orElseGet(() -> ResponseEntity.ok(new LastSyncResponse(null, false)));
    }

    record LastSyncResponse(Instant lastMeasuredAt, boolean hasReadings) {}
}
