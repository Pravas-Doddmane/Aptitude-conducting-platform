package com.PassFamilyDoddmane.QuizeBackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistrationOtpRequest(@Email @NotBlank String email) {
}
