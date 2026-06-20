package com.PassFamilyDoddmane.QuizeBackend.dto.question;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.DifficultyLevel;

import java.util.List;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        UUID categoryId,
        String stem,
        String explanation,
        String imageUrl,
        DifficultyLevel difficultyLevel,
        String status,
        List<QuestionOptionResponse> options
) {
}
