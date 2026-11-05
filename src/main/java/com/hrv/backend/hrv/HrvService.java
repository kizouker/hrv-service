package com.hrv.backend.hrv;

import com.hrv.backend.exception.ResourceNotFoundException;
import com.hrv.backend.hrv.dto.*;
import com.hrv.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrvService {

    private final HrvRepository hrvRepository;

    @Transactional
    public HrvReadingResponse create(User user, HrvReadingRequest req) {
        HrvReading reading = HrvReading.builder()
                .user(user)
                .deviceId(req.deviceId())
                // DB timestamps keep microsecond precision; truncate so stored
                // values match what duplicate detection compares later.
                .measuredAt(req.measuredAt().truncatedTo(ChronoUnit.MILLIS))
                .rmssd(req.rmssd())
                .sdnn(req.sdnn())
                .pnn50(req.pnn50())
                .meanRr(req.meanRr())
                .meanHr(req.meanHr())
                .lfPower(req.lfPower())
                .hfPower(req.hfPower())
                .lfHfRatio(req.lfHfRatio())
                .notes(req.notes())
                .build();
        return HrvReadingResponse.from(hrvRepository.save(reading));
    }

    public List<HrvReadingResponse> getAll(User user) {
        return hrvRepository.findAllByUserIdOrderByMeasuredAtDesc(user.getId())
                .stream().map(HrvReadingResponse::from).toList();
    }

    public HrvReadingResponse getById(User user, Long id) {
        return HrvReadingResponse.from(
                hrvRepository.findByIdAndUserId(id, user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Reading not found")));
    }

    @Transactional
    public HrvReadingResponse update(User user, Long id, HrvReadingRequest req) {
        HrvReading reading = hrvRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Reading not found"));
        reading.setDeviceId(req.deviceId());
        reading.setMeasuredAt(req.measuredAt().truncatedTo(ChronoUnit.MILLIS));
        reading.setRmssd(req.rmssd());
        reading.setSdnn(req.sdnn());
        reading.setPnn50(req.pnn50());
        reading.setMeanRr(req.meanRr());
        reading.setMeanHr(req.meanHr());
        reading.setLfPower(req.lfPower());
        reading.setHfPower(req.hfPower());
        reading.setLfHfRatio(req.lfHfRatio());
        reading.setNotes(req.notes());
        return HrvReadingResponse.from(hrvRepository.save(reading));
    }

    @Transactional
    public void delete(User user, Long id) {
        HrvReading reading = hrvRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Reading not found"));
        hrvRepository.delete(reading);
    }
    public HrvAnalyticsSummary getSummary(User user, int periodDays) {
        Instant from = Instant.now().minus(periodDays, ChronoUnit.DAYS);
        List<Object[]> results = hrvRepository.aggregateStats(user.getId(), from);

        if (results.isEmpty()) {
            return new HrvAnalyticsSummary(periodDays, 0, null, null, null, null, null, null, null);
        }

        Object[] row = results.get(0);
        Double avgRmssd = toDouble(row[0]);
        Double avgSdnn  = toDouble(row[1]);
        Double avgPnn50 = toDouble(row[2]);
        Double avgHr    = toDouble(row[3]);
        Double minRmssd = toDouble(row[4]);
        Double maxRmssd = toDouble(row[5]);
        long   count    = row[6] != null ? ((Number) row[6]).longValue() : 0L;

        List<Object[]> b30 = hrvRepository.aggregateStats(user.getId(),
                Instant.now().minus(30, ChronoUnit.DAYS));
        Double baseline = b30.isEmpty() ? null : toDouble(b30.get(0)[0]);

        return new HrvAnalyticsSummary(periodDays, count,
                avgRmssd, avgSdnn, avgPnn50, avgHr, minRmssd, maxRmssd, baseline);
    }

    public List<HrvTrendPoint> getTrend(User user, int periodDays) {
        Instant to   = Instant.now();
        Instant from = to.minus(periodDays, ChronoUnit.DAYS);
        return hrvRepository.dailyAverages(user.getId(), from, to)
                .stream()
                .map(row -> new HrvTrendPoint(
                        toLocalDate(row[0]),
                        toDouble(row[1]),
                        toDouble(row[2]),
                        toDouble(row[3])))
                .toList();
    }

    private Double toDouble(Object o) {
        return o instanceof Number n ? n.doubleValue() : null;
    }

    private LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        return null;
    }
}
