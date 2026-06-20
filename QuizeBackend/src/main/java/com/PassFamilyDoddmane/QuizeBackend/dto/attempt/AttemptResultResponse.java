package com.PassFamilyDoddmane.QuizeBackend.dto.attempt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AttemptResultResponse(
        UUID attemptId,
        UUID quizId,
        UUID quizVersionId,
        Integer score,
        Integer maxScore,
        Integer correctCount,
        Integer wrongCount,
        Integer unansweredCount,
        Integer elapsedSeconds,
        Instant startedAt,
        Instant submittedAt,
        Boolean autoSubmitted,
        String autoSubmitReason,
        Boolean resultAvailable,
        Integer passingScore,
        List<AttemptAnswerResponse> answers,
        List<DetailedAttemptAnswerResponse> detailedAnswers
) {
}
