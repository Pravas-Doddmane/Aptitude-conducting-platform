package com.PassFamilyDoddmane.QuizeBackend.dto.user;

import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        Boolean active,
        String status
) {
}
