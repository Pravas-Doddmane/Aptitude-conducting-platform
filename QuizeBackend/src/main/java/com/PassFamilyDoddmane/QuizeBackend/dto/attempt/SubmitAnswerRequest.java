package com.PassFamilyDoddmane.QuizeBackend.dto.attempt;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitAnswerRequest(
        @NotNull UUID questionId,
        UUID selectedOptionId,
        Long responseTimeMs
) {
}
