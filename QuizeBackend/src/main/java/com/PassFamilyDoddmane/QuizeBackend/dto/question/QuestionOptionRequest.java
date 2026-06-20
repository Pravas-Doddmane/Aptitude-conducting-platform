package com.PassFamilyDoddmane.QuizeBackend.dto.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionOptionRequest(
        @NotNull Integer optionOrder,
        @NotBlank @Size(max = 2000) String optionText,
        @NotNull Boolean correct
) {
}
