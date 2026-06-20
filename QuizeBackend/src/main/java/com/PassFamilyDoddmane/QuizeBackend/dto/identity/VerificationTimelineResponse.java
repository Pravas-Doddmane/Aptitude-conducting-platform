package com.PassFamilyDoddmane.QuizeBackend.dto.identity;

import java.time.Instant;
import java.util.UUID;

public record VerificationTimelineResponse(
        UUID attemptId,
        Instant timestamp,
        String result,
        Double matchScore,
        Boolean triggeredViolation
) {
}
