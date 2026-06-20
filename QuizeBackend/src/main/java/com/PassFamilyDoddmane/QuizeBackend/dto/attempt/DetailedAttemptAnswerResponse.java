package com.PassFamilyDoddmane.QuizeBackend.dto.attempt;

import java.util.List;
import java.util.UUID;

public record DetailedAttemptAnswerResponse(
        UUID questionId,
        String questionText,
        String questionImageUrl,
        String explanation,
        UUID selectedOptionId,
        String selectedOptionText,
        UUID correctOptionId,
        String correctOptionText,
        Boolean isCorrect,
        Long responseTimeMs,
        List<QuestionOptionDetail> allOptions
) {
    public record QuestionOptionDetail(
            UUID optionId,
            String optionText,
            Integer optionOrder,
            Boolean isCorrect
    ) {}
}
