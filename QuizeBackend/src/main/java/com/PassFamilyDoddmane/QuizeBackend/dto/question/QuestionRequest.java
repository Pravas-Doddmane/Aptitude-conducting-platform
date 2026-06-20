package com.PassFamilyDoddmane.QuizeBackend.dto.question;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.DifficultyLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record QuestionRequest(
        @NotNull UUID categoryId,
        @Size(max = 4000) String explanation,
        String imageUrl,
        @NotBlank @Size(max = 4000) String stem,
        @NotNull DifficultyLevel difficultyLevel,
        @Valid @Size(min = 2, max = 6, message = "Each question must have between 2 and 6 options") List<QuestionOptionRequest> options
) {
}
