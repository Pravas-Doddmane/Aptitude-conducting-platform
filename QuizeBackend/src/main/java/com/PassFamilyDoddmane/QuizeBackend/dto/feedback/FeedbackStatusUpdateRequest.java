package com.PassFamilyDoddmane.QuizeBackend.dto.feedback;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.FeedbackStatus;
import jakarta.validation.constraints.NotNull;

public record FeedbackStatusUpdateRequest(
        @NotNull FeedbackStatus status,
        String adminNote
) {
}
