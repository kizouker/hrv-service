package com.hrv.backend.hrv;

import com.hrv.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A single HRV measurement session.
 *
 * Key metrics:
 *  rmssd   – Root Mean Square of Successive Differences (ms). Most popular HRV metric.
 *  sdnn    – Std deviation of NN intervals (ms). Overall HRV.
 *  pnn50   – % of successive NN intervals differing >50 ms. Parasympathetic activity.
 *  meanRr  – Mean RR interval (ms).
 *  meanHr  – Mean heart rate (bpm).
 *  lfPower – Low-frequency power (ms²) — sympathetic/vagal mix.
 *  hfPower – High-frequency power (ms²) — parasympathetic / vagal tone.
 *  lfHfRatio – Autonomic balance indicator.
 */
@Entity
@Table(name = "hrv_readings", indexes = {
        @Index(name = "idx_hrv_user_measured", columnList = "user_id, measured_at DESC")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HrvReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Which device produced this reading (nullable = manually entered) */
    @Column(name = "device_id")
    private Long deviceId;

    /** When the measurement was taken (client-provided timestamp) */
    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    // ── Time-domain metrics ──────────────────────────────────────────────────
    private Double rmssd;
    private Double sdnn;
    private Double pnn50;
    private Double meanRr;
    private Double meanHr;

    // ── Frequency-domain metrics (optional) ──────────────────────────────────
    private Double lfPower;
    private Double hfPower;
    private Double lfHfRatio;

    /** Free-text notes or context (e.g. "after morning meditation") */
    @Column(length = 1000)
    private String notes;

    @Column(updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { this.createdAt = Instant.now(); }
}
