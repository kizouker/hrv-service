package com.hrv.backend.hrv;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface HrvRepository extends JpaRepository<HrvReading, Long> {
    List<HrvReading> findAllByUserIdOrderByMeasuredAtDesc(Long userId);

    Optional<HrvReading> findByIdAndUserId(Long id, Long userId);

    List<HrvReading> findAllByUserIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
            Long userId, Instant from, Instant to);

    @Query("""
            SELECT AVG(h.rmssd), AVG(h.sdnn), AVG(h.pnn50), AVG(h.meanHr),
                   MIN(h.rmssd), MAX(h.rmssd), COUNT(h)
            FROM HrvReading h
            WHERE h.user.id = :userId
              AND h.measuredAt >= :from
            """)
    List<Object[]> aggregateStats(@Param("userId") Long userId, @Param("from") Instant from);
    // UTC date, not the database session TimeZone. CAST(timestamptz AS date)
    // follows that session zone and can split one hour across two days.
    @Query("""
            SELECT CAST(FUNCTION('timezone', 'UTC', h.measuredAt) AS date),
                   AVG(h.rmssd), AVG(h.sdnn), AVG(h.meanHr)
            FROM HrvReading h
            WHERE h.user.id = :userId
              AND h.measuredAt BETWEEN :from AND :to
            GROUP BY CAST(FUNCTION('timezone', 'UTC', h.measuredAt) AS date)
            ORDER BY CAST(FUNCTION('timezone', 'UTC', h.measuredAt) AS date) ASC
            """)
    List<Object[]> dailyAverages(
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
