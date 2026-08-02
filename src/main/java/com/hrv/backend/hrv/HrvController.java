package com.hrv.backend.hrv;

import com.hrv.backend.hrv.dto.*;
import com.hrv.backend.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hrv")
@RequiredArgsConstructor
@Tag(name = "HRV Readings", description = "Submit, retrieve and delete HRV measurements")
@SecurityRequirement(name = "bearerAuth")
public class HrvController {

    private final HrvService hrvService;

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Submit a new HRV reading")
    public ResponseEntity<HrvReadingResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody HrvReadingRequest req) {
        return ResponseEntity.status(201).body(hrvService.create(user, req));
    }

    @GetMapping
    @Operation(summary = "Get all readings (newest first)")
    public List<HrvReadingResponse> getAll(@AuthenticationPrincipal User user) {
        return hrvService.getAll(user);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single reading by ID")
    public HrvReadingResponse getById(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return hrvService.getById(user, id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a reading")
    public HrvReadingResponse update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody HrvReadingRequest req) {
        return hrvService.update(user, id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a reading")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        hrvService.delete(user, id);
        return ResponseEntity.noContent().build();
    }

    // ── Analytics ────────────────────────────────────────────────────────────

    @GetMapping("/analytics/summary")
    @Operation(summary = "Aggregated stats (avg/min/max RMSSD, baseline, etc.)")
    public HrvAnalyticsSummary summary(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Look-back window in days", example = "30")
            @RequestParam(defaultValue = "30") int days) {
        return hrvService.getSummary(user, days);
    }

    @GetMapping("/analytics/trend")
    @Operation(summary = "Daily averages for trend chart")
    public List<HrvTrendPoint> trend(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Look-back window in days", example = "30")
            @RequestParam(defaultValue = "30") int days) {
        return hrvService.getTrend(user, days);
    }
}
