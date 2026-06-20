package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.AttemptStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.AutoSubmitReason;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.VerificationResult;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.VerificationStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.ViolationType;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.BadRequestException;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.dto.identity.CaptureReferenceImageRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.identity.IdentityVerificationResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.identity.RecordVerificationAttemptRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.identity.VerificationAttemptResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.identity.VerificationTimelineResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.RecordViolationRequest;
import com.PassFamilyDoddmane.QuizeBackend.entity.IdentityVerification;
import com.PassFamilyDoddmane.QuizeBackend.entity.ProctoringSession;
import com.PassFamilyDoddmane.QuizeBackend.entity.QuizAttempt;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import com.PassFamilyDoddmane.QuizeBackend.entity.VerificationAttempt;
import com.PassFamilyDoddmane.QuizeBackend.repository.IdentityVerificationRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.ProctoringSessionRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizAttemptRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.VerificationAttemptRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class IdentityVerificationService {

    private final IdentityVerificationRepository identityVerificationRepository;
    private final VerificationAttemptRepository verificationAttemptRepository;
    private final ProctoringSessionRepository proctoringSessionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ProctoringService proctoringService;
    private final CurrentUserService currentUserService;

    /**
     * Capture reference image for identity verification
     */
    public IdentityVerificationResponse captureReferenceImage(UUID attemptId, CaptureReferenceImageRequest request) {
        User user = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found"));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Cannot capture reference image for completed attempt");
        }

        ProctoringSession session = proctoringSessionRepository.findByQuizAttemptId(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Proctoring session not found"));

        // Check if identity verification already exists
        IdentityVerification existing = identityVerificationRepository.findByProctoringSessionId(session.getId())
                .orElse(null);

        if (existing != null) {
            throw new BadRequestException("Reference image already captured for this attempt");
        }

        // Create identity verification
        IdentityVerification verification = new IdentityVerification();
        verification.setProctoringSession(session);
        verification.setUser(user);
        verification.setReferenceImageUrl(request.referenceImageUrl());
        verification.setReferenceCapturedAt(Instant.now());
        verification.setVerificationStatus(VerificationStatus.PENDING);

        verification = identityVerificationRepository.save(verification);
        return toIdentityVerificationResponse(verification);
    }

    /**
     * Record a verification attempt (comparison result)
     */
    public VerificationAttemptResponse recordVerificationAttempt(
            UUID attemptId, 
            RecordVerificationAttemptRequest request
    ) {
        User user = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found"));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Cannot record verification for completed attempt");
        }

        IdentityVerification verification = identityVerificationRepository.findByAttemptId(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Identity verification not found. Capture reference image first."));

        // Create verification attempt
        VerificationAttempt verificationAttempt = new VerificationAttempt();
        verificationAttempt.setIdentityVerification(verification);
        verificationAttempt.setCapturedImageUrl(request.capturedImageUrl());
        verificationAttempt.setVerificationTimestamp(request.verificationTimestamp());
        verificationAttempt.setVerificationResult(VerificationResult.valueOf(request.verificationResult().toUpperCase()));
        verificationAttempt.setMatchScore(request.matchScore());
        verificationAttempt.setConfidenceLevel(request.confidenceLevel());
        verificationAttempt.setProcessingTimeMs(request.processingTimeMs());
        verificationAttempt.setDetails(request.details());
        verificationAttempt.setEvidenceUrl(request.evidenceUrl());

        VerificationResult result = VerificationResult.valueOf(request.verificationResult().toUpperCase());
        boolean isFailure = result == VerificationResult.NOT_MATCHED || 
                           result == VerificationResult.LOW_CONFIDENCE;

        // Update verification statistics
        verification.setTotalVerifications(verification.getTotalVerifications() + 1);
        if (result == VerificationResult.MATCHED) {
            verification.setSuccessfulVerifications(verification.getSuccessfulVerifications() + 1);
        } else {
            verification.setFailedVerifications(verification.getFailedVerifications() + 1);
        }

        // Update match score statistics
        if (request.matchScore() != null) {
            Double avgScore = verificationAttemptRepository.calculateAverageMatchScore(verification.getId());
            verification.setAverageMatchScore(avgScore != null ? avgScore : request.matchScore());
            
            if (verification.getLowestMatchScore() == null || 
                request.matchScore() < verification.getLowestMatchScore()) {
                verification.setLowestMatchScore(request.matchScore());
            }
        }

        verification.setLastVerificationAt(Instant.now());

        // If identity mismatch, record as violation
        if (isFailure) {
            RecordViolationRequest violationRequest = new RecordViolationRequest(
                    ViolationType.IDENTITY_MISMATCH.name(),
                    request.verificationTimestamp(),
                    "Identity verification failed: " + result.name(),
                    request.capturedImageUrl(),
                    request.evidenceUrl(),
                    String.format("{\"result\":\"%s\",\"matchScore\":%.2f}", 
                                result.name(), 
                                request.matchScore() != null ? request.matchScore() : 0.0),
                    1  // Deduct 1 life
            );

            var violationResponse = proctoringService.recordViolation(attemptId, violationRequest);
            verificationAttempt.setTriggeredViolation(Boolean.TRUE);
            verificationAttempt.setViolationId(violationResponse.violationId());
        }

        verificationAttempt = verificationAttemptRepository.save(verificationAttempt);
        identityVerificationRepository.save(verification);

        return toVerificationAttemptResponse(verificationAttempt);
    }

    /**
     * Get identity verification for an attempt
     */
    public IdentityVerificationResponse getVerification(UUID attemptId) {
        User user = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found"));

        IdentityVerification verification = identityVerificationRepository.findByAttemptId(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Identity verification not found"));

        return toIdentityVerificationResponse(verification);
    }

    /**
     * Get verification timeline
     */
    public List<VerificationTimelineResponse> getVerificationTimeline(UUID attemptId) {
        User user = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found"));

        IdentityVerification verification = identityVerificationRepository.findByAttemptId(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Identity verification not found"));

        List<VerificationAttempt> attempts = verificationAttemptRepository
                .findByIdentityVerificationIdOrderByVerificationTimestampAsc(verification.getId());

        return attempts.stream()
                .map(va -> new VerificationTimelineResponse(
                        va.getId(),
                        va.getVerificationTimestamp(),
                        va.getVerificationResult().name(),
                        va.getMatchScore(),
                        va.getTriggeredViolation()
                ))
                .toList();
    }

    /**
     * Update verification status
     */
    public IdentityVerificationResponse updateVerificationStatus(UUID attemptId, VerificationStatus status) {
        User user = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found"));

        IdentityVerification verification = identityVerificationRepository.findByAttemptId(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Identity verification not found"));

        verification.setVerificationStatus(status);
        if (status == VerificationStatus.VERIFIED) {
            verification.setVerifiedAt(Instant.now());
        }

        verification = identityVerificationRepository.save(verification);
        return toIdentityVerificationResponse(verification);
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
}
