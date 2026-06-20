package com.PassFamilyDoddmane.QuizeBackend.controller;

import com.PassFamilyDoddmane.QuizeBackend.dto.identity.DetailedIdentityReport;
import com.PassFamilyDoddmane.QuizeBackend.dto.identity.IdentityVerificationResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.identity.VerificationAttemptResponse;
import com.PassFamilyDoddmane.QuizeBackend.service.AdminIdentityVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/identity-verification")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminIdentityVerificationController {

    private final AdminIdentityVerificationService adminIdentityVerificationService;

    /**
     * Get detailed identity verification report
     * GET /api/admin/identity-verification/attempts/{attemptId}/report
     */
    @GetMapping("/attempts/{attemptId}/report")
    public ResponseEntity<DetailedIdentityReport> getDetailedIdentityReport(@PathVariable UUID attemptId) {
        return ResponseEntity.ok(adminIdentityVerificationService.getDetailedIdentityReport(attemptId));
    }

    /**
     * Get all identity verifications for a quiz
     * GET /api/admin/identity-verification/quizzes/{quizId}/verifications
     */
    @GetMapping("/quizzes/{quizId}/verifications")
    public ResponseEntity<List<IdentityVerificationResponse>> getVerificationsByQuiz(@PathVariable UUID quizId) {
        return ResponseEntity.ok(adminIdentityVerificationService.getVerificationsByQuiz(quizId));
    }

    /**
     * Get all identity verifications for a user
     * GET /api/admin/identity-verification/users/{userId}/verifications
     */
    @GetMapping("/users/{userId}/verifications")
    public ResponseEntity<List<IdentityVerificationResponse>> getVerificationsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminIdentityVerificationService.getVerificationsByUser(userId));
    }

    /**
     * Get all verification attempts with identity mismatches
     * GET /api/admin/identity-verification/mismatches
     */
    @GetMapping("/mismatches")
    public ResponseEntity<List<VerificationAttemptResponse>> getMismatchedVerifications() {
        return ResponseEntity.ok(adminIdentityVerificationService.getMismatchedVerifications());
    }

    /**
     * Get all verification attempts for a specific verification
     * GET /api/admin/identity-verification/{verificationId}/attempts
     */
    @GetMapping("/{verificationId}/attempts")
    public ResponseEntity<List<VerificationAttemptResponse>> getVerificationAttempts(@PathVariable UUID verificationId) {
        return ResponseEntity.ok(adminIdentityVerificationService.getVerificationAttempts(verificationId));
    }
}
