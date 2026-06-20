package com.PassFamilyDoddmane.QuizeBackend.dto.attempt;

import java.util.UUID;

public record AttemptAnswerResponse(
        UUID questionId,
        UUID selectedOptionId,
        Boolean correct,
        Long responseTimeMs
) {
}
