package com.PassFamilyDoddmane.QuizeBackend.dto.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RecordVerificationAttemptRequest(
        @NotBlank(message = "Captured image URL is required")
        String capturedImageUrl,
        
        @NotNull(message = "Verification timestamp is required")
        Instant verificationTimestamp,
        
        @NotBlank(message = "Verification result is required")
        String verificationResult,
        
        Double matchScore,
        Double confidenceLevel,
        Long processingTimeMs,
        String details,
        String evidenceUrl
) {
}
