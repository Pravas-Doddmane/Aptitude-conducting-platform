package com.PassFamilyDoddmane.QuizeBackend.dto.identity;

import java.util.List;
import java.util.UUID;

public record DetailedIdentityReport(
        UUID attemptId,
        String userEmail,
        String userName,
        String quizTitle,
        IdentityVerificationResponse verification,
        List<VerificationAttemptResponse> attempts,
        List<VerificationTimelineResponse> timeline,
        Integer totalMismatches,
        Double averageMatchScore
) {
}
