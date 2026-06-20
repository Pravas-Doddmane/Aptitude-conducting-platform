package com.PassFamilyDoddmane.QuizeBackend.dto.auth;

import java.util.List;
import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        List<String> roles,
        String accessToken,
        String refreshToken
) {
}
