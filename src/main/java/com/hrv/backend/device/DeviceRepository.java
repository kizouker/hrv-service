package com.hrv.backend.device;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findAllByUserId(Long userId);
    Optional<Device> findByIdAndUserId(Long id, Long userId);
}
