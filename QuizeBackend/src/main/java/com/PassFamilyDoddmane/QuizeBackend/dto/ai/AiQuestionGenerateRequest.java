package com.PassFamilyDoddmane.QuizeBackend.dto.ai;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.DifficultyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AiQuestionGenerateRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 300) String topic,
        @NotNull DifficultyLevel difficultyLevel,
        @NotNull @Min(1) @Max(20) Integer questionCount,
        @NotNull @Min(2) @Max(6) Integer optionsPerQuestion
) {
}
