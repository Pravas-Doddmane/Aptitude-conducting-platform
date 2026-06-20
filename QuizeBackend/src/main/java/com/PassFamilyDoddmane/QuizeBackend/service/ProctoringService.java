package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.AttemptStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.AutoSubmitReason;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.EventSeverity;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.ProctoringSessionStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.RiskLevel;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.ViolationType;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.BadRequestException;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.DetailedProctoringReport;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.ProctoringEventResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.ProctoringSessionResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.ProctoringViolationResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.RecordEventRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.RecordViolationRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.ViolationTimelineResponse;
import com.PassFamilyDoddmane.QuizeBackend.entity.ProctoringEvent;
import com.PassFamilyDoddmane.QuizeBackend.entity.ProctoringSession;
import com.PassFamilyDoddmane.QuizeBackend.entity.ProctoringViolation;
import com.PassFamilyDoddmane.QuizeBackend.entity.Quiz;
import com.PassFamilyDoddmane.QuizeBackend.entity.QuizAttempt;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import com.PassFamilyDoddmane.QuizeBackend.repository.ProctoringEventRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.ProctoringSessionRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.ProctoringViolationRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizAttemptRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProctoringService {

    private final ProctoringSessionRepository proctoringSessionRepository;
    private final ProctoringEventRepository proctoringEventRepository;
    private final ProctoringViolationRepository proctoringViolationRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final CurrentUserService currentUserService;

    /**
     * Initialize proctoring session for a quiz attempt
     */
    public ProctoringSessionResponse initializeProctoringSession(UUID attemptId) {
        User user = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found"));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Cannot initialize proctoring for a completed attempt");
        }

        // Check if session already exists
        ProctoringSession existingSession = proctoringSessionRepository.findByQuizAttemptId(attemptId)
                .orElse(null);
        
        if (existingSession != null) {
            return toProctoringSessionResponse(existingSession);
        }

        Quiz quiz = attempt.getQuizVersion().getQuiz();
        Integer initialLives = quiz.getInitialLives() != null ? quiz.getInitialLives() : 3;

        ProctoringSession session = new ProctoringSession();
        session.setQuizAttempt(attempt);
        session.setStartedAt(Instant.now());
        session.setStatus(ProctoringSessionStatus.ACTIVE);
        session.setInitialLives(initialLives);
        session.setRemainingLives(initialLives);
        session.setProctoringEnabled(Boolean.TRUE.equals(quiz.getProctoringEnabled()));
        session.setRiskScore(0);
        session.setRiskLevel(RiskLevel.LOW);

        session = proctoringSessionRepository.save(session);
        
        // Update attempt
        attempt.setProctoringEnabled(Boolean.TRUE);
        quizAttemptRepository.save(attempt);

        return toProctoringSessionResponse(session);
    }

    /**
     * Record a proctoring event
     */
    public ProctoringEventResponse recordEvent(UUID attemptId, RecordEventRequest request) {
        User user = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found"));

        ProctoringSession session = proctoringSessionRepository.findByQuizAttemptId(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Proctoring session not found"));

        if (session.getStatus() != ProctoringSessionStatus.ACTIVE) {
            throw new BadRequestException("Proctoring session is not active");
        }

        ProctoringEvent event = new ProctoringEvent();
        event.setProctoringSession(session);
        event.setEventType(request.eventType());
        event.setSeverity(EventSeverity.valueOf(request.severity().toUpperCase()));
        event.setEventTimestamp(request.eventTimestamp());
        event.setDescription(request.description());
        event.setFrameDataUrl(request.frameDataUrl());
        event.setConfidenceScore(request.confidenceScore());
        event.setMetadata(request.metadata());
        
        int livesDeducted = request.livesDeducted() != null ? request.livesDeducted() : 0;
        event.setLivesDeducted(livesDeducted);
        event.setRemainingLivesSnapshot(session.getRemainingLives());

        event = proctoringEventRepository.save(event);
        return toProctoringEventResponse(event);
    }

    /**
     * Record a violation and deduct lives
     */
    public ProctoringViolationResponse recordViolation(UUID attemptId, RecordViolationRequest request) {
        User user = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found"));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Cannot record violation for a completed attempt");
        }

        ProctoringSession session = proctoringSessionRepository.findByQuizAttemptId(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Proctoring session not found"));

        if (session.getStatus() != ProctoringSessionStatus.ACTIVE) {
            throw new BadRequestException("Proctoring session is not active");
        }

        // Deduct lives
        int livesDeducted = request.livesDeducted() != null ? request.livesDeducted() : 1;
        int remainingLives = Math.max(0, session.getRemainingLives() - livesDeducted);
        
        // Create violation record
        ProctoringViolation violation = new ProctoringViolation();
        violation.setProctoringSession(session);
        violation.setViolationType(ViolationType.valueOf(request.violationType().toUpperCase()));
        violation.setViolationTimestamp(request.violationTimestamp());
        violation.setDescription(request.description());
        violation.setSnapshotUrl(request.snapshotUrl());
        violation.setEvidenceUrl(request.evidenceUrl());
        violation.setEvidenceData(request.evidenceData());
        violation.setLivesDeducted(livesDeducted);
        violation.setRemainingLivesAfter(remainingLives);
        violation.setAutoFlagged(Boolean.TRUE);
        violation.setReviewed(Boolean.FALSE);

        violation = proctoringViolationRepository.save(violation);

        // Update session
        session.setRemainingLives(remainingLives);
        session.setTotalViolations(session.getTotalViolations() + 1);
        
        if (livesDeducted > 0) {
            session.setCriticalViolations(session.getCriticalViolations() + 1);
        } else {
            session.setWarningViolations(session.getWarningViolations() + 1);
        }

        // Update risk score and level
        updateRiskAssessment(session);
        proctoringSessionRepository.save(session);

        // Auto-submit if no lives remaining
        if (remainingLives <= 0) {
            autoSubmitDueToViolation(attempt, request.violationType());
        }

        return toProctoringViolationResponse(violation);
    }

    /**
     * Get proctoring session for an attempt
     */
    public ProctoringSessionResponse getSession(UUID attemptId) {
        User user = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found"));

        ProctoringSession session = proctoringSessionRepository.findByQuizAttemptId(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Proctoring session not found"));

        return toProctoringSessionResponse(session);
    }

    /**
     * End proctoring session
     */
    public ProctoringSessionResponse endSession(UUID attemptId) {
        User user = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found"));

        ProctoringSession session = proctoringSessionRepository.findByQuizAttemptId(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Proctoring session not found"));

        if (session.getStatus() == ProctoringSessionStatus.ACTIVE) {
            session.setStatus(ProctoringSessionStatus.COMPLETED);
            session.setEndedAt(Instant.now());
            proctoringSessionRepository.save(session);
        }

        return toProctoringSessionResponse(session);
    }

    /**
     * Get violation timeline for an attempt
     */
    public List<ViolationTimelineResponse> getViolationTimeline(UUID attemptId) {
        User user = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found"));

        ProctoringSession session = proctoringSessionRepository.findByQuizAttemptId(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Proctoring session not found"));

        List<ProctoringViolation> violations = proctoringViolationRepository
                .findByProctoringSessionIdOrderByViolationTimestampAsc(session.getId());

        return violations.stream()
                .map(v -> new ViolationTimelineResponse(
                        v.getId(),
                        v.getViolationType().name(),
                        v.getViolationTimestamp(),
                        v.getDescription(),
                        v.getLivesDeducted(),
                        v.getRemainingLivesAfter()
                ))
                .toList();
    }

    /**
     * Auto-submit attempt due to violation
     */
    private void autoSubmitDueToViolation(QuizAttempt attempt, String violationType) {
        AutoSubmitReason reason = mapViolationToAutoSubmitReason(violationType);
        
        attempt.setStatus(AttemptStatus.AUTO_SUBMITTED);
        attempt.setSubmittedAt(Instant.now());
        attempt.setAutoSubmitted(Boolean.TRUE);
        attempt.setAutoSubmitReason(reason);
        
        quizAttemptRepository.save(attempt);

        // End proctoring session
        ProctoringSession session = proctoringSessionRepository.findByQuizAttemptId(attempt.getId())
                .orElse(null);
        if (session != null && session.getStatus() == ProctoringSessionStatus.ACTIVE) {
            session.setStatus(ProctoringSessionStatus.TERMINATED);
            session.setEndedAt(Instant.now());
            session.setNotes("Auto-submitted due to: " + reason.name());
            proctoringSessionRepository.save(session);
        }
    }

    /**
     * Map violation type to auto-submit reason
     */
    private AutoSubmitReason mapViolationToAutoSubmitReason(String violationType) {
        return switch (violationType.toUpperCase()) {
            case "MULTIPLE_FACES" -> AutoSubmitReason.MULTIPLE_FACE_VIOLATION;
            case "PHONE_DETECTED" -> AutoSubmitReason.PHONE_DETECTED;
            case "TAB_SWITCH" -> AutoSubmitReason.TAB_SWITCH_VIOLATION;
            case "FULLSCREEN_EXIT" -> AutoSubmitReason.FULLSCREEN_EXIT_VIOLATION;
            case "SUSPICIOUS_ACTIVITY" -> AutoSubmitReason.SUSPICIOUS_ACTIVITY;
            default -> AutoSubmitReason.LIFE_LIMIT_EXCEEDED;
        };
    }

    /**
     * Update risk assessment for a session
     */
    private void updateRiskAssessment(ProctoringSession session) {
        int riskScore = 0;
        
        // Calculate risk score based on violations
        riskScore += session.getCriticalViolations() * 20;
        riskScore += session.getWarningViolations() * 5;
        
        // Calculate based on remaining lives
        int livesLost = session.getInitialLives() - session.getRemainingLives();
        riskScore += livesLost * 15;
        
        session.setRiskScore(Math.min(riskScore, 100));
        
        // Determine risk level
        RiskLevel riskLevel;
        if (riskScore >= 70) {
            riskLevel = RiskLevel.CRITICAL;
        } else if (riskScore >= 40) {
            riskLevel = RiskLevel.HIGH;
        } else if (riskScore >= 20) {
            riskLevel = RiskLevel.MEDIUM;
        } else {
            riskLevel = RiskLevel.LOW;
        }
        
        session.setRiskLevel(riskLevel);
    }

    // Response mappers
    private ProctoringSessionResponse toProctoringSessionResponse(ProctoringSession session) {
        return new ProctoringSessionResponse(
                session.getId(),
                session.getQuizAttempt().getId(),
                session.getStatus().name(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getInitialLives(),
                session.getRemainingLives(),
                session.getTotalViolations(),
                session.getCriticalViolations(),
                session.getWarningViolations(),
                session.getRiskScore(),
                session.getRiskLevel().name(),
                session.getProctoringEnabled(),
                session.getNotes()
        );
    }

    private ProctoringEventResponse toProctoringEventResponse(ProctoringEvent event) {
        return new ProctoringEventResponse(
                event.getId(),
                event.getProctoringSession().getId(),
                event.getEventType(),
                event.getSeverity().name(),
                event.getEventTimestamp(),
                event.getDescription(),
                event.getFrameDataUrl(),
                event.getConfidenceScore(),
                event.getLivesDeducted(),
                event.getRemainingLivesSnapshot()
        );
    }

    private ProctoringViolationResponse toProctoringViolationResponse(ProctoringViolation violation) {
        return new ProctoringViolationResponse(
                violation.getId(),
                violation.getProctoringSession().getId(),
                violation.getViolationType().name(),
                violation.getViolationTimestamp(),
                violation.getDescription(),
                violation.getSnapshotUrl(),
                violation.getEvidenceUrl(),
                violation.getLivesDeducted(),
                violation.getRemainingLivesAfter(),
                violation.getAutoFlagged(),
                violation.getReviewed(),
                violation.getReviewedAt(),
                violation.getReviewerNotes()
        );
    }
}
