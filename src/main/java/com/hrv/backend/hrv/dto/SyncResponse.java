package com.hrv.backend.hrv.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a batch sync operation")
public record SyncResponse(
        @Schema(example = "12") int received,
        @Schema(example = "10") int saved,
        @Schema(example = "2")  int skippedDuplicates
) {}
