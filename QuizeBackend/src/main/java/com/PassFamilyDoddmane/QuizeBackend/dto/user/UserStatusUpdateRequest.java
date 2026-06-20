package com.PassFamilyDoddmane.QuizeBackend.dto.user;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserStatusUpdateRequest(
        @NotNull UUID userId,
        @NotNull UserStatus status
) {
}
