package com.hrv.backend.device;

import com.hrv.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "devices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;               // e.g. "My Polar H10"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType type;

    private String externalDeviceId;   // device serial / UUID from vendor

    @Column(updatable = false)
    private Instant registeredAt;

    @PrePersist
    void prePersist() { this.registeredAt = Instant.now(); }

    public enum DeviceType {
        APPLE_WATCH, GARMIN, POLAR, WHOOP, OURA, MANUAL, OTHER
    }
}
