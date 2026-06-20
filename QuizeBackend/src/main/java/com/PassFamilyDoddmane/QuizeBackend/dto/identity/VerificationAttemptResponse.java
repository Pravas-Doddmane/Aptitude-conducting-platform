package com.PassFamilyDoddmane.QuizeBackend.dto.identity;

import java.time.Instant;
import java.util.UUID;

public record VerificationAttemptResponse(
        UUID attemptId,
        UUID verificationId,
        String capturedImageUrl,
        Instant verificationTimestamp,
        String verificationResult,
        Double matchScore,
        Double confidenceLevel,
        Long processingTimeMs,
        String details,
        String evidenceUrl,
        Boolean triggeredViolation,
        UUID violationId
) {
}
