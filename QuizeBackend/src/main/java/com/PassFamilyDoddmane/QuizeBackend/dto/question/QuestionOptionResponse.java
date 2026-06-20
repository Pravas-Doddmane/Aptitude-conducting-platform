package com.PassFamilyDoddmane.QuizeBackend.dto.question;

import java.util.UUID;

public record QuestionOptionResponse(
        UUID id,
        Integer optionOrder,
        String optionText,
        Boolean correct
) {
}
