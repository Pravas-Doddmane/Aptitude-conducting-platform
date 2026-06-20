package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.dto.identity.DetailedIdentityReport;
import com.PassFamilyDoddmane.QuizeBackend.dto.identity.IdentityVerificationResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.identity.VerificationAttemptResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.identity.VerificationTimelineResponse;
import com.PassFamilyDoddmane.QuizeBackend.entity.IdentityVerification;
import com.PassFamilyDoddmane.QuizeBackend.entity.QuizAttempt;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import com.PassFamilyDoddmane.QuizeBackend.entity.UserProfile;
import com.PassFamilyDoddmane.QuizeBackend.entity.VerificationAttempt;
import com.PassFamilyDoddmane.QuizeBackend.repository.IdentityVerificationRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizAttemptRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.VerificationAttemptRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminIdentityVerificationService {

    private final IdentityVerificationRepository identityVerificationRepository;
    private final VerificationAttemptRepository verificationAttemptRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    /**
     * Get detailed identity verification report for an attempt
     */
    public DetailedIdentityReport getDetailedIdentityReport(UUID attemptId) {
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found"));

        IdentityVerification verification = identityVerificationRepository.findByAttemptId(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Identity verification not found"));

        List<VerificationAttempt> attempts = verificationAttemptRepository
                .findByIdentityVerificationIdOrderByVerificationTimestampAsc(verification.getId());

        User user = attempt.getUser();
        UserProfile profile = user.getProfile();
        String userName = profile != null ? 
                profile.getFirstName() + " " + profile.getLastName() : "N/A";

        int totalMismatches = verification.getFailedVerifications();
        Double avgMatchScore = verification.getAverageMatchScore();

        return new DetailedIdentityReport(
                attemptId,
                user.getEmail(),
                userName,
                attempt.getQuizVersion().getTitleSnapshot(),
                toIdentityVerificationResponse(verification),
                attempts.stream().map(this::toVerificationAttemptResponse).toList(),
                attempts.stream().map(this::toVerificationTimelineResponse).toList(),
                totalMismatches,
                avgMatchScore
        );
    }

    /**
     * Get all identity verifications for a quiz
     */
    public List<IdentityVerificationResponse> getVerificationsByQuiz(UUID quizId) {
        List<IdentityVerification> verifications = identityVerificationRepository.findByQuizId(quizId);
        return verifications.stream()
                .map(this::toIdentityVerificationResponse)
                .toList();
    }

    /**
     * Get all identity verifications for a user
     */
    public List<IdentityVerificationResponse> getVerificationsByUser(UUID userId) {
        List<IdentityVerification> verifications = identityVerificationRepository.findByUserId(userId);
        return verifications.stream()
                .map(this::toIdentityVerificationResponse)
                .toList();
    }

    /**
     * Get all verification attempts with mismatches
     */
    public List<VerificationAttemptResponse> getMismatchedVerifications() {
        List<VerificationAttempt> attempts = verificationAttemptRepository.findByTriggeredViolationTrue();
        return attempts.stream()
                .map(this::toVerificationAttemptResponse)
                .toList();
    }

    /**
     * Get verification attempts for a specific verification
     */
    public List<VerificationAttemptResponse> getVerificationAttempts(UUID verificationId) {
        IdentityVerification verification = identityVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Identity verification not found"));

        List<VerificationAttempt> attempts = verificationAttemptRepository
                .findByIdentityVerificationIdOrderByVerificationTimestampAsc(verificationId);

        return attempts.stream()
                .map(this::toVerificationAttemptResponse)
                .toList();
    }

    // Response mappers
    private IdentityVerificationResponse toIdentityVerificationResponse(IdentityVerification verification) {
        return new IdentityVerificationResponse(
                verification.getId(),
                verification.getProctoringSession().getId(),
                verification.getUser().getId(),
                verification.getReferenceImageUrl(),
                verification.getReferenceCapturedAt(),
                verification.getVerificationStatus().name(),
                verification.getVerifiedAt(),
                verification.getTotalVerifications(),
                verification.getSuccessfulVerifications(),
                verification.getFailedVerifications(),
                verification.getAverageMatchScore(),
                verification.getLowestMatchScore(),
                verification.getLastVerificationAt(),
                verification.getNotes()
        );
    }

    private VerificationAttemptResponse toVerificationAttemptResponse(VerificationAttempt attempt) {
        return new VerificationAttemptResponse(
                attempt.getId(),
                attempt.getIdentityVerification().getId(),
                attempt.getCapturedImageUrl(),
                attempt.getVerificationTimestamp(),
                attempt.getVerificationResult().name(),
                attempt.getMatchScore(),
                attempt.getConfidenceLevel(),
                attempt.getProcessingTimeMs(),
                attempt.getDetails(),
                attempt.getEvidenceUrl(),
                attempt.getTriggeredViolation(),
                attempt.getViolationId()
        );
    }

    private VerificationTimelineResponse toVerificationTimelineResponse(VerificationAttempt attempt) {
        return new VerificationTimelineResponse(
                attempt.getId(),
                attempt.getVerificationTimestamp(),
                attempt.getVerificationResult().name(),
                attempt.getMatchScore(),
                attempt.getTriggeredViolation()
        );
    }
}
