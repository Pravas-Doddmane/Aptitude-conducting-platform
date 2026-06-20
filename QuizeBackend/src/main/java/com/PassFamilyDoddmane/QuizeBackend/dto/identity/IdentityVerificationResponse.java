package com.PassFamilyDoddmane.QuizeBackend.dto.identity;

import java.time.Instant;
import java.util.UUID;

public record IdentityVerificationResponse(
        UUID verificationId,
        UUID sessionId,
        UUID userId,
        String referenceImageUrl,
        Instant referenceCapturedAt,
        String verificationStatus,
        Instant verifiedAt,
        Integer totalVerifications,
        Integer successfulVerifications,
        Integer failedVerifications,
        Double averageMatchScore,
        Double lowestMatchScore,
        Instant lastVerificationAt,
        String notes
) {
}
