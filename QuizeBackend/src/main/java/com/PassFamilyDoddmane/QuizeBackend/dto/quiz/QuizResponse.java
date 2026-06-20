package com.PassFamilyDoddmane.QuizeBackend.dto.quiz;

import java.util.UUID;
import java.time.Instant;

public record QuizResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        java.util.List<UUID> categoryIds,
        java.util.List<String> categoryNames,
        String title,
        String slug,
        String description,
        String status,
        Integer durationSeconds,
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
        UUID certificateTemplateId,
        String certificateTemplateName,
        UUID currentVersionId,
        Integer questionCount,
        Integer easyQuestionCount,
        Integer mediumQuestionCount,
        Integer hardQuestionCount,
        Boolean currentlyAvailable
) {
}
