package com.PassFamilyDoddmane.QuizeBackend.dto.attempt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StartAttemptResponse(
        UUID attemptId,
        UUID quizId,
        UUID quizVersionId,
        Instant startedAt,
        Integer durationSeconds,
        List<AttemptQuestionResponse> questions
) {
}
