package com.hrv.backend.device;

import com.hrv.backend.device.dto.DeviceRequest;
import com.hrv.backend.device.dto.DeviceResponse;
import com.hrv.backend.exception.ResourceNotFoundException;
import com.hrv.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceResponse register(User user, DeviceRequest req) {
        Device device = Device.builder()
                .user(user)
                .name(req.name())
                .type(req.type())
                .externalDeviceId(req.externalDeviceId())
                .build();
        return DeviceResponse.from(deviceRepository.save(device));
    }

    public List<DeviceResponse> listForUser(User user) {
        return deviceRepository.findAllByUserId(user.getId())
                .stream().map(DeviceResponse::from).toList();
    }

    public void delete(User user, Long deviceId) {
        Device device = deviceRepository.findByIdAndUserId(deviceId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
        deviceRepository.delete(device);
    }
}
