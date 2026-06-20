package com.PassFamilyDoddmane.QuizeBackend.repository;

import com.PassFamilyDoddmane.QuizeBackend.entity.RegistrationOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationOtpRepository extends JpaRepository<RegistrationOtp, UUID> {
    Optional<RegistrationOtp> findTopByEmailAndOtpHashOrderByCreatedAtDesc(String email, String otpHash);
    Optional<RegistrationOtp> findByVerificationTokenHash(String verificationTokenHash);
}
