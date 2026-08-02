package com.hrv.backend.hrv.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Aggregated HRV statistics over a time window")
public record HrvAnalyticsSummary(
        @Schema(example = "30") int periodDays,
        @Schema(example = "42") long readingCount,
        @Schema(example = "58.2") Double avgRmssd,
        @Schema(example = "51.0") Double avgSdnn,
        @Schema(example = "24.1") Double avgPnn50,
        @Schema(example = "71.5") Double avgMeanHr,
        @Schema(example = "32.1") Double minRmssd,
        @Schema(example = "88.7") Double maxRmssd,
        @Schema(description = "30-day RMSSD baseline") Double baseline
) {}
