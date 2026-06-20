package com.PassFamilyDoddmane.QuizeBackend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthRefreshRequest(@NotBlank String refreshToken) {
}
