package com.PassFamilyDoddmane.QuizeBackend.dto.proctoring;

import java.time.Instant;
import java.util.UUID;

public record ViolationTimelineResponse(
        UUID violationId,
        String violationType,
        Instant timestamp,
        String description,
        Integer livesDeducted,
        Integer remainingLives
) {
}
