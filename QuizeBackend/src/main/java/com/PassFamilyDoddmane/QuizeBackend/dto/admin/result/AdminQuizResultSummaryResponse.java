package com.PassFamilyDoddmane.QuizeBackend.dto.admin.result;

import java.util.UUID;

public record AdminQuizResultSummaryResponse(
        UUID quizId,
        String quizTitle,
        String categoryName,
        Integer questionCount,
        Integer passingScore,
        long totalParticipants,
        long passedParticipants,
        long failedParticipants
) {
}
