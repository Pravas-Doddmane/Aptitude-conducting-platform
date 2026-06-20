package com.PassFamilyDoddmane.QuizeBackend.dto.feedback;

import java.time.Instant;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        UUID userId,
        String userName,
        String email,
        UUID quizId,
        String quizTitle,
        UUID questionId,
        String questionStem,
        Integer rating,
        String message,
        String status,
        String adminNote,
        UUID reviewedById,
        String reviewedByName,
        Instant reviewedAt,
        Instant createdAt
) {
}
