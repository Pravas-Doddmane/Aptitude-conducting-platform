package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.AttemptStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.AutoSubmitReason;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.QuizStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.BadRequestException;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.dto.attempt.AttemptAnswerResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.attempt.AttemptOptionResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.attempt.AttemptQuestionResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.attempt.AttemptResultResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.attempt.AttemptSummaryResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.attempt.DetailedAttemptAnswerResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.attempt.StartAttemptResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.attempt.SubmitAnswerRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.attempt.SubmitAttemptRequest;
import com.PassFamilyDoddmane.QuizeBackend.entity.AttemptAnswer;
import com.PassFamilyDoddmane.QuizeBackend.entity.Question;
import com.PassFamilyDoddmane.QuizeBackend.entity.QuestionOption;
import com.PassFamilyDoddmane.QuizeBackend.entity.Quiz;
import com.PassFamilyDoddmane.QuizeBackend.entity.QuizAttempt;
import com.PassFamilyDoddmane.QuizeBackend.entity.QuizVersionQuestion;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import com.PassFamilyDoddmane.QuizeBackend.repository.AttemptAnswerRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuestionOptionRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizAttemptRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizVersionQuestionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AttemptService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRepository quizRepository;
    private final QuizVersionQuestionRepository quizVersionQuestionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final CurrentUserService currentUserService;

    public StartAttemptResponse startAttempt(UUID quizId) {
        User user = currentUserService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        if (quiz.getStatus() != QuizStatus.PUBLISHED || quiz.getCurrentVersion() == null) {
            throw new BadRequestException("Quiz is not published");
        }
        ensureQuizCurrentlyAvailable(quiz);
        
        // Check for existing IN_PROGRESS attempt for this quiz
        List<QuizAttempt> userAttempts = quizAttemptRepository.findByUserIdOrderByStartedAtDesc(user.getId());
        QuizAttempt existingAttempt = userAttempts.stream()
                .filter(a -> a.getQuizVersion().getQuiz().getId().equals(quizId))
                .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                .findFirst()
                .orElse(null);
        
        if (existingAttempt != null) {
            // Resume existing attempt
            return new StartAttemptResponse(
                    existingAttempt.getId(),
                    quiz.getId(),
                    quiz.getCurrentVersion().getId(),
                    existingAttempt.getStartedAt(),
                    quiz.getDurationSeconds(),
                    getPlayableQuestions(quiz.getCurrentVersion().getId())
            );
        }
        
        // No IN_PROGRESS attempt found, create new one
        Integer maxAttemptsPerUser = quiz.getMaxAttemptsPerUser();
        long attemptCount = quizAttemptRepository.countByUserIdAndQuizVersionQuizId(user.getId(), quizId);
        if (maxAttemptsPerUser != null && maxAttemptsPerUser > 0 && attemptCount >= maxAttemptsPerUser) {
            throw new BadRequestException("Attempt limit reached for this quiz");
        }
        int attemptNumber = quizAttemptRepository.findTopByUserIdAndQuizVersionQuizIdOrderByAttemptNumberDesc(user.getId(), quizId)
                .map(previous -> previous.getAttemptNumber() + 1)
                .orElse(1);
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUser(user);
        attempt.setQuizVersion(quiz.getCurrentVersion());
        attempt.setStartedAt(Instant.now());
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setAttemptNumber(attemptNumber);
        attempt = quizAttemptRepository.save(attempt);
        return new StartAttemptResponse(
                attempt.getId(),
                quiz.getId(),
                quiz.getCurrentVersion().getId(),
                attempt.getStartedAt(),
                quiz.getDurationSeconds(),
                getPlayableQuestions(quiz.getCurrentVersion().getId())
        );
    }

    public AttemptResultResponse submitAttempt(UUID attemptId, SubmitAttemptRequest request) {
        User user = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Attempt already submitted");
        }
        return doSubmit(attempt, request.answers(), false, null);
    }

    public AttemptResultResponse autoSubmit(UUID attemptId, SubmitAttemptRequest request) {
        User user = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Attempt already submitted");
        }
        List<SubmitAnswerRequest> answers = request != null && request.answers() != null ? request.answers() : List.of();
        return doSubmit(attempt, answers, true, AutoSubmitReason.TIME_EXPIRED);
    }

    public List<AttemptSummaryResponse> myAttempts() {
        User user = currentUserService.getCurrentUser();
        return quizAttemptRepository.findByUserIdOrderByStartedAtDesc(user.getId()).stream().map(this::toSummary).toList();
    }

    public AttemptResultResponse getResult(UUID attemptId) {
        User user = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));
        return toResult(attempt);
    }

    private void ensureQuizCurrentlyAvailable(Quiz quiz) {
        Instant now = Instant.now();
        if (quiz.getAvailableFrom() != null && now.isBefore(quiz.getAvailableFrom())) {
            throw new BadRequestException("Quiz is not available yet");
        }
        if (quiz.getAvailableTo() != null && now.isAfter(quiz.getAvailableTo())) {
            throw new BadRequestException("Quiz availability has ended");
        }
    }

    private List<AttemptQuestionResponse> getPlayableQuestions(UUID quizVersionId) {
        return quizVersionQuestionRepository.findByQuizVersionIdOrderByDisplayOrderAsc(quizVersionId).stream()
                .map(link -> new AttemptQuestionResponse(
                        link.getQuestion().getId(),
                        link.getQuestion().getStem(),
                        link.getQuestion().getImageUrl(),
                        link.getQuestion().getDifficultyLevel().name(),
                        questionOptionRepository.findByQuestionIdOrderByOptionOrderAsc(link.getQuestion().getId()).stream()
                                .map(option -> new AttemptOptionResponse(
                                        option.getId(),
                                        option.getOptionOrder(),
                                        option.getOptionText()
                                ))
                                .toList()
                ))
                .toList();
    }

    private AttemptResultResponse doSubmit(
            QuizAttempt attempt,
            List<SubmitAnswerRequest> answers,
            boolean forcedAutoSubmit,
            AutoSubmitReason forcedAutoSubmitReason
    ) {
        Instant now = Instant.now();
        int elapsed = (int) Duration.between(attempt.getStartedAt(), now).toSeconds();
        int duration = attempt.getQuizVersion().getDurationSeconds();
        boolean autoSubmitted = forcedAutoSubmit || elapsed >= duration;
        AutoSubmitReason autoSubmitReason = null;
        if (autoSubmitted) {
            autoSubmitReason = forcedAutoSubmitReason != null ? forcedAutoSubmitReason : AutoSubmitReason.TIME_EXPIRED;
        }

        List<QuizVersionQuestion> expectedQuestions = quizVersionQuestionRepository.findByQuizVersionIdOrderByDisplayOrderAsc(attempt.getQuizVersion().getId());
        Map<UUID, SubmitAnswerRequest> answerMap = new HashMap<>();
        for (SubmitAnswerRequest answer : answers) {
            answerMap.put(answer.questionId(), answer);
        }

        int score = 0;
        int correct = 0;
        int wrong = 0;
        int unanswered = 0;
        List<AttemptAnswer> savedAnswers = new ArrayList<>();

        for (QuizVersionQuestion quizQuestion : expectedQuestions) {
            SubmitAnswerRequest submittedAnswer = answerMap.get(quizQuestion.getQuestion().getId());
            AttemptAnswer attemptAnswer = new AttemptAnswer();
            attemptAnswer.setAttempt(attempt);
            attemptAnswer.setQuestion(quizQuestion.getQuestion());
            attemptAnswer.setAnsweredAt(now);
            attemptAnswer.setResponseTimeMs(submittedAnswer == null ? null : submittedAnswer.responseTimeMs());
            attemptAnswer.setQuestionSnapshotJson("{\"questionId\":\"" + quizQuestion.getQuestion().getId() + "\"}");

            if (submittedAnswer == null || submittedAnswer.selectedOptionId() == null) {
                unanswered++;
                attemptAnswer.setCorrect(Boolean.FALSE);
            } else {
                QuestionOption selectedOption = questionOptionRepository.findByIdAndQuestionId(submittedAnswer.selectedOptionId(), quizQuestion.getQuestion().getId())
                        .orElseThrow(() -> new BadRequestException("Selected option does not belong to the question"));
                attemptAnswer.setSelectedOption(selectedOption);
                boolean isCorrect = Boolean.TRUE.equals(selectedOption.getCorrect());
                attemptAnswer.setCorrect(isCorrect);
                if (isCorrect) {
                    correct++;
                    score += quizQuestion.getMarks();
                } else {
                    wrong++;
                    score = Math.max(0, score - quizQuestion.getNegativeMarks());
                }
            }
            savedAnswers.add(attemptAnswer);
        }

        attemptAnswerRepository.saveAll(savedAnswers);
        attempt.setScore(score);
        attempt.setMaxScore(expectedQuestions.stream().mapToInt(QuizVersionQuestion::getMarks).sum());
        attempt.setCorrectCount(correct);
        attempt.setWrongCount(wrong);
        attempt.setUnansweredCount(unanswered);
        attempt.setSubmittedAt(now);
        attempt.setElapsedSeconds(Math.min(elapsed, duration));
        attempt.setAutoSubmitted(autoSubmitted);
        attempt.setAutoSubmitReason(autoSubmitReason);
        attempt.setStatus(autoSubmitted ? AttemptStatus.AUTO_SUBMITTED : AttemptStatus.SUBMITTED);
        quizAttemptRepository.save(attempt);
        return toResult(attempt);
    }

    private AttemptSummaryResponse toSummary(QuizAttempt attempt) {
        return new AttemptSummaryResponse(
                attempt.getId(),
                attempt.getQuizVersion().getQuiz().getId(),
                attempt.getQuizVersion().getTitleSnapshot(),
                attempt.getScore(),
                attempt.getMaxScore(),
                attempt.getElapsedSeconds(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getStatus().name(),
                attempt.getAutoSubmitted(),
                attempt.getAutoSubmitReason() != null ? attempt.getAutoSubmitReason().name() : null,
                isResultAvailable(attempt)
        );
    }

    private AttemptResultResponse toResult(QuizAttempt attempt) {
        List<AttemptAnswer> attemptAnswers = attemptAnswerRepository.findByAttemptIdOrderByCreatedAtAsc(attempt.getId());
        
        List<AttemptAnswerResponse> answerResponses = attemptAnswers.stream()
                .map(answer -> new AttemptAnswerResponse(
                        answer.getQuestion().getId(),
                        answer.getSelectedOption() == null ? null : answer.getSelectedOption().getId(),
                        answer.getCorrect(),
                        answer.getResponseTimeMs()
                ))
                .toList();
        
        // Build detailed answers with full question and option details
        List<DetailedAttemptAnswerResponse> detailedAnswers = attemptAnswers.stream()
                .map(answer -> {
                    Question question = answer.getQuestion();
                    QuestionOption selectedOption = answer.getSelectedOption();
                    
                    // Find the correct option
                    QuestionOption correctOption = question.getOptions().stream()
                            .filter(opt -> Boolean.TRUE.equals(opt.getCorrect()))
                            .findFirst()
                            .orElse(null);
                    
                    // Build all options list
                    List<DetailedAttemptAnswerResponse.QuestionOptionDetail> allOptions = 
                            question.getOptions().stream()
                                    .map(opt -> new DetailedAttemptAnswerResponse.QuestionOptionDetail(
                                            opt.getId(),
                                            opt.getOptionText(),
                                            opt.getOptionOrder(),
                                            opt.getCorrect()
                                    ))
                                    .toList();
                    
                    return new DetailedAttemptAnswerResponse(
                            question.getId(),
                            question.getStem(),
                            question.getImageUrl(),
                            question.getExplanation(),
                            selectedOption == null ? null : selectedOption.getId(),
                            selectedOption == null ? null : selectedOption.getOptionText(),
                            correctOption == null ? null : correctOption.getId(),
                            correctOption == null ? null : correctOption.getOptionText(),
                            answer.getCorrect(),
                            answer.getResponseTimeMs(),
                            allOptions
                    );
                })
                .toList();
        
        return new AttemptResultResponse(
                attempt.getId(),
                attempt.getQuizVersion().getQuiz().getId(),
                attempt.getQuizVersion().getId(),
                attempt.getScore(),
                attempt.getMaxScore(),
                attempt.getCorrectCount(),
                attempt.getWrongCount(),
                attempt.getUnansweredCount(),
                attempt.getElapsedSeconds(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getAutoSubmitted(),
                attempt.getAutoSubmitReason() != null ? attempt.getAutoSubmitReason().name() : null,
                isResultAvailable(attempt),
                attempt.getQuizVersion().getPassingScore(),
                answerResponses,
                detailedAnswers
        );
    }

    private boolean isResultAvailable(QuizAttempt attempt) {
        if (!Boolean.TRUE.equals(attempt.getAutoSubmitted())) {
            return true;
        }
        return attempt.getAutoSubmitReason() == null || attempt.getAutoSubmitReason() == AutoSubmitReason.TIME_EXPIRED;
    }
}
