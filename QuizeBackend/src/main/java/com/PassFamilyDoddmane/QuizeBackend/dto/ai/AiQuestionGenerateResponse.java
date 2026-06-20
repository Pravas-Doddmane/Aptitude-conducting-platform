package com.PassFamilyDoddmane.QuizeBackend.dto.ai;

import com.PassFamilyDoddmane.QuizeBackend.dto.question.QuestionResponse;

import java.util.List;
import java.util.UUID;

public record AiQuestionGenerateResponse(
        UUID categoryId,
        String categoryName,
        int requestedCount,
        int createdCount,
        int skippedDuplicateCount,
        List<QuestionResponse> questions
) {
}
