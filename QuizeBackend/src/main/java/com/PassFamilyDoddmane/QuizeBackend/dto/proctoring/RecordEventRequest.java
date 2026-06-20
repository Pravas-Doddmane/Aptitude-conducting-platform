package com.PassFamilyDoddmane.QuizeBackend.dto.proctoring;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RecordEventRequest(
        @NotBlank(message = "Event type is required")
        String eventType,
        
        @NotBlank(message = "Severity is required")
        String severity,
        
        @NotNull(message = "Event timestamp is required")
        Instant eventTimestamp,
        
        String description,
        String frameDataUrl,
        Double confidenceScore,
        String metadata,
        Integer livesDeducted
) {
}
