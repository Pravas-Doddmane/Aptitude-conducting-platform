package com.PassFamilyDoddmane.QuizeBackend.controller;

import com.PassFamilyDoddmane.QuizeBackend.dto.auth.AuthLoginRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.AuthRefreshRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.AuthRegisterRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.AuthResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.ForgotPasswordRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.ForgotPasswordResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.RegistrationOtpRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.RegistrationOtpResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.ResetPasswordRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.VerifyRegistrationOtpRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.VerifyRegistrationOtpResponse;
import com.PassFamilyDoddmane.QuizeBackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/register/send-otp")
    public ResponseEntity<RegistrationOtpResponse> sendRegistrationOtp(@Valid @RequestBody RegistrationOtpRequest request) {
        return ResponseEntity.ok(authService.sendRegistrationOtp(request.email()));
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<VerifyRegistrationOtpResponse> verifyRegistrationOtp(@Valid @RequestBody VerifyRegistrationOtpRequest request) {
        return ResponseEntity.ok(authService.verifyRegistrationOtp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody AuthRefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody AuthRefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request.email()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
