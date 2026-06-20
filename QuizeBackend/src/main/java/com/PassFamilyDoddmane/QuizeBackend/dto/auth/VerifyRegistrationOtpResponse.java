package com.PassFamilyDoddmane.QuizeBackend.dto.auth;

public record VerifyRegistrationOtpResponse(
        String verificationToken,
        String message
) {
}
