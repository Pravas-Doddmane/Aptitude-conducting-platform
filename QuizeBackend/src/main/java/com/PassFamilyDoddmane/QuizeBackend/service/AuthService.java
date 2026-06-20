package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.RoleName;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.UserStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.AuditAction;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.BadRequestException;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.ConflictException;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.AuthLoginRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.AuthRefreshRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.AuthRegisterRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.AuthResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.ForgotPasswordResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.RegistrationOtpResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.ResetPasswordRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.VerifyRegistrationOtpRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.auth.VerifyRegistrationOtpResponse;
import com.PassFamilyDoddmane.QuizeBackend.entity.PasswordResetToken;
import com.PassFamilyDoddmane.QuizeBackend.entity.RefreshToken;
import com.PassFamilyDoddmane.QuizeBackend.entity.RegistrationOtp;
import com.PassFamilyDoddmane.QuizeBackend.entity.Role;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import com.PassFamilyDoddmane.QuizeBackend.entity.UserProfile;
import com.PassFamilyDoddmane.QuizeBackend.entity.UserRole;
import com.PassFamilyDoddmane.QuizeBackend.repository.PasswordResetTokenRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.RefreshTokenRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.RegistrationOtpRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.RoleRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.UserProfileRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.UserRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.UserRoleRepository;
import com.PassFamilyDoddmane.QuizeBackend.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RegistrationOtpRepository registrationOtpRepository;
    private final MailService mailService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(AuthRegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }
        RegistrationOtp verification = registrationOtpRepository.findByVerificationTokenHash(sha256(request.emailVerificationToken()))
                .orElseThrow(() -> new BadRequestException("Verify your email before registration"));
        if (!email.equals(verification.getEmail())
                || verification.getVerifiedAt() == null
                || verification.getUsedAt() != null
                || verification.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Email verification expired or invalid");
        }

        Role userRole = roleRepository.findByCode(RoleName.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Default role USER not found"));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        userProfileRepository.save(profile);

        UserRole membership = new UserRole();
        membership.setUser(user);
        membership.setRole(userRole);
        userRoleRepository.save(membership);

        verification.setUsedAt(Instant.now());
        registrationOtpRepository.save(verification);

        return buildAuthResponse(user);
    }

    public RegistrationOtpResponse sendRegistrationOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email is already registered");
        }
        String otp = generateSixDigitOtp();
        RegistrationOtp registrationOtp = new RegistrationOtp();
        registrationOtp.setEmail(normalizedEmail);
        registrationOtp.setOtpHash(sha256(otp));
        registrationOtp.setExpiresAt(Instant.now().plusSeconds(900));
        registrationOtpRepository.save(registrationOtp);
        mailService.sendRegistrationOtp(normalizedEmail, otp);
        return new RegistrationOtpResponse("OTP sent to your email. Please check it once.");
    }

    public VerifyRegistrationOtpResponse verifyRegistrationOtp(VerifyRegistrationOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        RegistrationOtp registrationOtp = registrationOtpRepository.findTopByEmailAndOtpHashOrderByCreatedAtDesc(normalizedEmail, sha256(request.otp()))
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));
        if (registrationOtp.getUsedAt() != null || registrationOtp.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("OTP expired or already used");
        }
        String verificationToken = UUID.randomUUID().toString();
        registrationOtp.setVerificationTokenHash(sha256(verificationToken));
        registrationOtp.setVerifiedAt(Instant.now());
        registrationOtpRepository.save(registrationOtp);
        return new VerifyRegistrationOtpResponse(verificationToken, "Email verified successfully. You can now register.");
    }

    public AuthResponse login(AuthLoginRequest request) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ensureAccountCanAuthenticate(user);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        AuthResponse response = buildAuthResponse(user);
        auditService.logSystemAction(
                AuditAction.LOGIN,
                "User",
                user.getId().toString(),
                null,
                Map.of("email", user.getEmail(), "status", user.getStatus().name())
        );
        return response;
    }

    public AuthResponse refresh(AuthRefreshRequest request) {
        String refreshToken = request.refreshToken();
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }
        String tokenHash = sha256(refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Refresh token not recognized"));
        if (storedToken.getRevokedAt() != null || storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token expired or revoked");
        }
        User user = storedToken.getUser();
        ensureAccountCanAuthenticate(user);

        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);
        AuthResponse response = buildAuthResponse(user);
        auditService.logSystemAction(
                AuditAction.REFRESH_TOKEN,
                "RefreshToken",
                storedToken.getId().toString(),
                null,
                Map.of("userId", user.getId(), "email", user.getEmail())
        );
        return response;
    }

    public void logout(AuthRefreshRequest request) {
        String tokenHash = sha256(request.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
            auditService.logSystemAction(
                    AuditAction.LOGOUT,
                    "RefreshToken",
                    token.getId().toString(),
                    null,
                    Map.of("userId", token.getUser().getId(), "email", token.getUser().getEmail())
            );
        });
    }

    public ForgotPasswordResponse forgotPassword(String email) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ensureAccountCanAuthenticate(user);
        String resetToken = generateSixDigitOtp();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(sha256(resetToken));
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        passwordResetTokenRepository.save(token);
        mailService.sendPasswordResetToken(user.getEmail(), resetToken);
        return new ForgotPasswordResponse(resetToken, "Use this 6-digit OTP to reset your password. It expires in 1 hour.");
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(sha256(request.token()))
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Reset token expired or used");
        }
        User user = token.getUser();
        ensureAccountCanAuthenticate(user);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);
    }

    private AuthResponse buildAuthResponse(User user) {
        List<String> roles = userRoleRepository.findByUserId(user.getId()).stream()
                .map(userRole -> "ROLE_" + userRole.getRole().getCode().name())
                .toList();

        String accessToken = jwtService.generateAccessToken(user, roles);
        String refreshToken = jwtService.generateRefreshToken(user, roles);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(user);
        storedToken.setTokenHash(sha256(refreshToken));
        storedToken.setExpiresAt(jwtService.getRefreshTokenExpiry());
        refreshTokenRepository.save(storedToken);

        return new AuthResponse(user.getId(), user.getEmail(), roles, accessToken, refreshToken);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private void ensureAccountCanAuthenticate(User user) {
        switch (user.getStatus()) {
            case ACTIVE -> {
            }
            case BLOCKED -> throw new BadRequestException("User account is blocked");
            case DELETED -> throw new BadRequestException("User account is deleted");
        }
    }

    private String generateSixDigitOtp() {
        int otp = ThreadLocalRandom.current().nextInt(100000, 1_000_000);
        return String.valueOf(otp);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
