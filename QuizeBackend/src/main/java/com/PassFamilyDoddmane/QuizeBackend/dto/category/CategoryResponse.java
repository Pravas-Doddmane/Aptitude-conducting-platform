package com.PassFamilyDoddmane.QuizeBackend.dto.category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        Integer sortOrder,
        Boolean active,
        Long questionCount,
        Long easyQuestionCount,
        Long mediumQuestionCount,
        Long hardQuestionCount
) {
}
