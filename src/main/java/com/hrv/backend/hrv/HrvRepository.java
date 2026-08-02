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
    @Query("""
            SELECT CAST(h.measuredAt AS date), AVG(h.rmssd), AVG(h.sdnn), AVG(h.meanHr)
            FROM HrvReading h
            WHERE h.user.id = :userId
              AND h.measuredAt BETWEEN :from AND :to
            GROUP BY CAST(h.measuredAt AS date)
            ORDER BY CAST(h.measuredAt AS date) ASC
            """)
    List<Object[]> dailyAverages(
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
