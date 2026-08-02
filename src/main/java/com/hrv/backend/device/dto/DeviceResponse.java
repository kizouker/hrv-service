package com.hrv.backend.device.dto;

import com.hrv.backend.device.Device;
import com.hrv.backend.device.Device.DeviceType;
import java.time.Instant;

public record DeviceResponse(
        Long id,
        String name,
        DeviceType type,
        String externalDeviceId,
        Instant registeredAt
) {
    public static DeviceResponse from(Device d) {
        return new DeviceResponse(d.getId(), d.getName(), d.getType(),
                d.getExternalDeviceId(), d.getRegisteredAt());
    }
}
