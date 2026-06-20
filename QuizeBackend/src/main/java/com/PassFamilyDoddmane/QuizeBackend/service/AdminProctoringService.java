package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.DetailedProctoringReport;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.ProctoringEventResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.ProctoringSessionResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.ProctoringViolationResponse;
import com.PassFamilyDoddmane.QuizeBackend.entity.ProctoringEvent;
import com.PassFamilyDoddmane.QuizeBackend.entity.ProctoringSession;
import com.PassFamilyDoddmane.QuizeBackend.entity.ProctoringViolation;
import com.PassFamilyDoddmane.QuizeBackend.entity.QuizAttempt;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import com.PassFamilyDoddmane.QuizeBackend.entity.UserProfile;
import com.PassFamilyDoddmane.QuizeBackend.repository.ProctoringEventRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.ProctoringSessionRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.ProctoringViolationRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizAttemptRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminProctoringService {

    private final ProctoringSessionRepository proctoringSessionRepository;
    private final ProctoringEventRepository proctoringEventRepository;
    private final ProctoringViolationRepository proctoringViolationRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    /**
     * Get detailed proctoring report for an attempt
     */
    public DetailedProctoringReport getDetailedReport(UUID attemptId) {
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found"));

        ProctoringSession session = proctoringSessionRepository.findByQuizAttemptId(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Proctoring session not found"));

        List<ProctoringViolation> violations = proctoringViolationRepository
                .findByProctoringSessionIdOrderByViolationTimestampAsc(session.getId());

        List<ProctoringEvent> events = proctoringEventRepository
                .findByProctoringSessionIdOrderByEventTimestampAsc(session.getId());

        User user = attempt.getUser();
        UserProfile profile = user.getProfile();
        String userName = profile != null ? 
                profile.getFirstName() + " " + profile.getLastName() : "N/A";

        return new DetailedProctoringReport(
                attemptId,
                user.getEmail(),
                userName,
                attempt.getQuizVersion().getTitleSnapshot(),
                toProctoringSessionResponse(session),
                violations.stream().map(this::toProctoringViolationResponse).toList(),
                events.stream().map(this::toProctoringEventResponse).toList(),
                attempt.getAutoSubmitReason() != null ? attempt.getAutoSubmitReason().name() : null,
                attempt.getAutoSubmitted()
        );
    }

    /**
     * Get all proctoring sessions for a quiz
     */
    public List<ProctoringSessionResponse> getSessionsByQuiz(UUID quizId) {
        List<ProctoringSession> sessions = proctoringSessionRepository.findByQuizId(quizId);
        return sessions.stream()
                .map(this::toProctoringSessionResponse)
                .toList();
    }

    /**
     * Get all proctoring sessions for a user
     */
    public List<ProctoringSessionResponse> getSessionsByUser(UUID userId) {
        List<ProctoringSession> sessions = proctoringSessionRepository.findByUserId(userId);
        return sessions.stream()
                .map(this::toProctoringSessionResponse)
                .toList();
    }

    /**
     * Get unreviewed violations
     */
    public List<ProctoringViolationResponse> getUnreviewedViolations() {
        List<ProctoringViolation> violations = proctoringViolationRepository
                .findByReviewedFalseOrderByViolationTimestampDesc();
        return violations.stream()
                .map(this::toProctoringViolationResponse)
                .toList();
    }

    /**
     * Mark violation as reviewed
     */
    public ProctoringViolationResponse markViolationReviewed(UUID violationId, String reviewerNotes) {
        ProctoringViolation violation = proctoringViolationRepository.findById(violationId)
                .orElseThrow(() -> new ResourceNotFoundException("Violation not found"));

        violation.setReviewed(Boolean.TRUE);
        violation.setReviewedAt(java.time.Instant.now());
        violation.setReviewerNotes(reviewerNotes);

        violation = proctoringViolationRepository.save(violation);
        return toProctoringViolationResponse(violation);
    }

    /**
     * Get violations for a specific quiz
     */
    public List<ProctoringViolationResponse> getViolationsByQuiz(UUID quizId) {
        List<ProctoringViolation> violations = proctoringViolationRepository.findByQuizId(quizId);
        return violations.stream()
                .map(this::toProctoringViolationResponse)
                .toList();
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
