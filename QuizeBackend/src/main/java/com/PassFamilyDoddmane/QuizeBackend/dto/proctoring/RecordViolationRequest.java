package com.PassFamilyDoddmane.QuizeBackend.dto.proctoring;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RecordViolationRequest(
        @NotBlank(message = "Violation type is required")
        String violationType,
        
        @NotNull(message = "Violation timestamp is required")
        Instant violationTimestamp,
        
        String description,
        String snapshotUrl,
        String evidenceUrl,
        String evidenceData,
        Integer livesDeducted
) {
}
