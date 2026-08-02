package com.hrv.backend.device;

import com.hrv.backend.device.dto.DeviceRequest;
import com.hrv.backend.device.dto.DeviceResponse;
import com.hrv.backend.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Tag(name = "Devices", description = "Register and manage measurement devices")
@SecurityRequirement(name = "bearerAuth")
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    @Operation(summary = "Register a new device")
    public ResponseEntity<DeviceResponse> register(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DeviceRequest req) {
        return ResponseEntity.status(201).body(deviceService.register(user, req));
    }

    @GetMapping
    @Operation(summary = "List all devices for the authenticated user")
    public List<DeviceResponse> list(@AuthenticationPrincipal User user) {
        return deviceService.listForUser(user);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a device")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        deviceService.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
