package com.hrv.backend.hrv.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Daily average for trend charts")
public record HrvTrendPoint(
        LocalDate date,
        Double avgRmssd,
        Double avgSdnn,
        Double avgMeanHr
) {}
