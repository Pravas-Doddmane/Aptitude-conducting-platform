package com.PassFamilyDoddmane.QuizeBackend.dto.attempt;

import java.util.List;
import java.util.UUID;

public record AttemptQuestionResponse(
        UUID id,
        String stem,
        String imageUrl,
        String difficultyLevel,
        List<AttemptOptionResponse> options
) {
}
