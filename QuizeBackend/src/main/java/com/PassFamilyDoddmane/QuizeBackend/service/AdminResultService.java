package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.QuizStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.dto.admin.result.AdminQuizParticipantResultResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.admin.result.AdminQuizResultSummaryResponse;
import com.PassFamilyDoddmane.QuizeBackend.entity.Quiz;
import com.PassFamilyDoddmane.QuizeBackend.entity.QuizAttempt;
import com.PassFamilyDoddmane.QuizeBackend.entity.UserProfile;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizAttemptRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminResultService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public List<AdminQuizResultSummaryResponse> summaries() {
        List<QuizAttempt> completedAttempts = completedAttempts();
        Map<UUID, List<QuizAttempt>> attemptsByQuiz = completedAttempts.stream()
                .collect(Collectors.groupingBy(attempt -> attempt.getQuizVersion().getQuiz().getId()));

        return quizRepository.findAll().stream()
                .filter(quiz -> quiz.getStatus() != QuizStatus.ARCHIVED)
                .map(quiz -> toSummary(quiz, attemptsByQuiz.getOrDefault(quiz.getId(), List.of())))
                .sorted(Comparator.comparing(AdminQuizResultSummaryResponse::quizTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<AdminQuizParticipantResultResponse> participantResults(UUID quizId, String filter) {
        quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));

        return completedAttempts().stream()
                .filter(attempt -> quizId.equals(attempt.getQuizVersion().getQuiz().getId()))
                .map(this::toParticipantResult)
                .filter(result -> matchesFilter(result, filter))
                .sorted(Comparator.comparing(AdminQuizParticipantResultResponse::submittedAt).reversed())
                .toList();
    }

    private List<QuizAttempt> completedAttempts() {
        return quizAttemptRepository.findAllWithRelations().stream()
                .filter(attempt -> attempt.getSubmittedAt() != null)
                .toList();
    }

    private AdminQuizResultSummaryResponse toSummary(Quiz quiz, List<QuizAttempt> attempts) {
        long passed = attempts.stream().filter(this::isPassed).count();
        Integer questionCount = quiz.getCurrentVersion() == null ? 0 : quiz.getCurrentVersion().getQuestionCount();
        Integer passingScore = quiz.getCurrentVersion() == null ? 0 : quiz.getCurrentVersion().getPassingScore();

        return new AdminQuizResultSummaryResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getCategory().getName(),
                questionCount,
                passingScore == null ? 0 : passingScore,
                attempts.size(),
                passed,
                attempts.size() - passed
        );
    }

    private AdminQuizParticipantResultResponse toParticipantResult(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuizVersion().getQuiz();
        UserProfile profile = attempt.getUser().getProfile();
        Integer elapsedSeconds = attempt.getElapsedSeconds() == null ? 0 : attempt.getElapsedSeconds();
        Integer questionCount = attempt.getQuizVersion().getQuestionCount() == null ? 0 : attempt.getQuizVersion().getQuestionCount();
        double averageSpeed = questionCount > 0 ? elapsedSeconds.doubleValue() / questionCount : 0.0;
        double scorePercent = scorePercent(attempt);

        return new AdminQuizParticipantResultResponse(
                attempt.getId(),
                quiz.getId(),
                quiz.getTitle(),
                quiz.getCategory().getName(),
                profile == null ? null : profile.getFirstName(),
                profile == null ? null : profile.getLastName(),
                attempt.getUser().getEmail(),
                averageSpeed,
                elapsedSeconds,
                attempt.getScore(),
                attempt.getMaxScore(),
                scorePercent,
                isPassed(attempt),
                attempt.getSubmittedAt()
        );
    }

    private boolean matchesFilter(AdminQuizParticipantResultResponse result, String filter) {
        if (filter == null || filter.isBlank() || "ALL".equalsIgnoreCase(filter)) {
            return true;
        }
        if ("PASSED".equalsIgnoreCase(filter)) {
            return Boolean.TRUE.equals(result.passed());
        }
        if ("FAILED".equalsIgnoreCase(filter)) {
            return !Boolean.TRUE.equals(result.passed());
        }
        return true;
    }

    private boolean isPassed(QuizAttempt attempt) {
        Integer passingScore = attempt.getQuizVersion().getPassingScore();
        return scorePercent(attempt) >= (passingScore == null ? 0 : passingScore);
    }

    private double scorePercent(QuizAttempt attempt) {
        if (attempt.getMaxScore() == null || attempt.getMaxScore() == 0) {
            return 0.0;
        }
        return attempt.getScore() * 100.0 / attempt.getMaxScore();
    }
}
