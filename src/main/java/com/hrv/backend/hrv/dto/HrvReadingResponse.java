package com.hrv.backend.hrv.dto;

import com.hrv.backend.hrv.HrvReading;
import java.time.Instant;

public record HrvReadingResponse(
        Long id,
        Long deviceId,
        Instant measuredAt,
        Double rmssd, Double sdnn, Double pnn50,
        Double meanRr, Double meanHr,
        Double lfPower, Double hfPower, Double lfHfRatio,
        String notes,
        Instant createdAt
) {
    public static HrvReadingResponse from(HrvReading r) {
        return new HrvReadingResponse(
                r.getId(), r.getDeviceId(), r.getMeasuredAt(),
                r.getRmssd(), r.getSdnn(), r.getPnn50(),
                r.getMeanRr(), r.getMeanHr(),
                r.getLfPower(), r.getHfPower(), r.getLfHfRatio(),
                r.getNotes(), r.getCreatedAt());
    }
}
