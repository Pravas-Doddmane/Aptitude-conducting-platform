package com.PassFamilyDoddmane.QuizeBackend.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record QuizRequest(
        @NotNull java.util.UUID categoryId,
        List<java.util.UUID> categoryIds,
        @NotBlank @Size(max = 160) String title,
        @Size(max = 2000) String description,
        @NotNull Integer durationSeconds,
        Integer maxAttemptsPerUser,
        Instant availableFrom,
        Instant availableTo,
        Boolean practiceMode,
        Boolean competitionMode,
        Boolean requireFullScreen,
        Boolean preventTabSwitch,
        Boolean requireCamera,
        Boolean requireMicrophone,
        Boolean requireLocation,
        Boolean certificateEnabled,
        Boolean certificateAutoGenerate,
        Integer certificateDelayHours,
        java.util.UUID certificateTemplateId,
        Integer passingScore,
        @Valid List<java.util.UUID> questionIds
) {
}
