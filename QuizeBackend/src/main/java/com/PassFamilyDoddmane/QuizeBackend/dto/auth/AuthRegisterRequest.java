package com.PassFamilyDoddmane.QuizeBackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @NotBlank String emailVerificationToken
) {
}
