package com.PassFamilyDoddmane.QuizeBackend.dto.attempt;

import java.util.UUID;

public record AttemptOptionResponse(
        UUID id,
        Integer optionOrder,
        String text
) {
}
