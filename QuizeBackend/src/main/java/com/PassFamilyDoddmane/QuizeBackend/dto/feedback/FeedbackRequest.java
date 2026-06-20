package com.PassFamilyDoddmane.QuizeBackend.dto.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FeedbackRequest(
        UUID quizId,
        UUID questionId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank String message
) {
}
