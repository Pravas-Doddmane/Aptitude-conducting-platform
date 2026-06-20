package com.PassFamilyDoddmane.QuizeBackend.dto.admin.result;

import java.time.Instant;
import java.util.UUID;

public record AdminQuizParticipantResultResponse(
        UUID attemptId,
        UUID quizId,
        String quizTitle,
        String categoryName,
        String firstName,
        String lastName,
        String email,
        Double averageSpeedSecondsPerQuestion,
        Integer totalTimeTakenSeconds,
        Integer score,
        Integer maxScore,
        Double scorePercent,
        Boolean passed,
        Instant submittedAt
) {
}
