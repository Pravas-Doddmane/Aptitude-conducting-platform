package com.PassFamilyDoddmane.QuizeBackend.dto.attempt;

import java.time.Instant;
import java.util.UUID;

public record AttemptSummaryResponse(
        UUID attemptId,
        UUID quizId,
        String quizTitle,
        Integer score,
        Integer maxScore,
        Integer elapsedSeconds,
        Instant startedAt,
        Instant submittedAt,
        String status,
        Boolean autoSubmitted,
        String autoSubmitReason,
        Boolean resultAvailable
) {
}
