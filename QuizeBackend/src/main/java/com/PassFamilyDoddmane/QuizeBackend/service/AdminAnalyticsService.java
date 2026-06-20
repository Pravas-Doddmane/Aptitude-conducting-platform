package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.QuestionStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.QuizStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.DifficultyLevel;
import com.PassFamilyDoddmane.QuizeBackend.dto.admin.dashboard.AdminDashboardResponse;
import com.PassFamilyDoddmane.QuizeBackend.entity.Category;
import com.PassFamilyDoddmane.QuizeBackend.entity.Question;
import com.PassFamilyDoddmane.QuizeBackend.entity.Feedback;
import com.PassFamilyDoddmane.QuizeBackend.entity.Quiz;
import com.PassFamilyDoddmane.QuizeBackend.entity.QuizAttempt;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import com.PassFamilyDoddmane.QuizeBackend.repository.CategoryRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.FeedbackRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuestionRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizAttemptRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.UserRepository;
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
public class AdminAnalyticsService {

    private final CategoryRepository categoryRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final FeedbackRepository feedbackRepository;

    public AdminDashboardResponse dashboard() {
        List<Category> categories = categoryRepository.findAll().stream()
                .filter(category -> Boolean.TRUE.equals(category.getActive()))
                .toList();
        List<Quiz> quizzes = quizRepository.findAll().stream()
                .filter(quiz -> quiz.getStatus() != QuizStatus.ARCHIVED)
                .toList();
        List<Question> questions = questionRepository.findAll();
        List<User> users = userRepository.findAll();
        List<QuizAttempt> attempts = quizAttemptRepository.findAllWithRelations();
        List<Feedback> feedbacks = feedbackRepository.findAll();

        Map<UUID, Long> quizCountByCategory = quizzes.stream()
                .collect(Collectors.groupingBy(quiz -> quiz.getCategory().getId(), Collectors.counting()));

        Map<UUID, List<Question>> questionsByCategory = questions.stream()
                .collect(Collectors.groupingBy(question -> question.getCategory().getId()));

        Map<UUID, Long> attemptCountByCategory = attempts.stream()
                .filter(attempt -> attempt.getQuizVersion() != null
                        && attempt.getQuizVersion().getQuiz() != null
                        && attempt.getQuizVersion().getQuiz().getCategory() != null)
                .collect(Collectors.groupingBy(
                        attempt -> attempt.getQuizVersion().getQuiz().getCategory().getId(),
                        Collectors.counting()
                ));

        Map<UUID, Long> attemptCountByQuiz = attempts.stream()
                .filter(attempt -> attempt.getQuizVersion() != null && attempt.getQuizVersion().getQuiz() != null)
                .collect(Collectors.groupingBy(
                        attempt -> attempt.getQuizVersion().getQuiz().getId(),
                        Collectors.counting()
                ));

        Map<UUID, List<QuizAttempt>> attemptsByUser = attempts.stream()
                .filter(attempt -> attempt.getSubmittedAt() != null)
                .collect(Collectors.groupingBy(attempt -> attempt.getUser().getId()));

        long completedAttempts = attempts.stream().filter(attempt -> attempt.getSubmittedAt() != null).count();
        long feedbackCount = feedbacks.size();
        double averageScorePercent = attempts.stream()
                .filter(attempt -> attempt.getSubmittedAt() != null && attempt.getMaxScore() != null && attempt.getMaxScore() > 0)
                .mapToDouble(attempt -> attempt.getScore() * 100.0 / attempt.getMaxScore())
                .average()
                .orElse(0.0);

        List<AdminDashboardResponse.CategoryAnalytics> categoryAnalytics = categories.stream()
                .map(category -> {
                    List<Question> categoryQuestions = questionsByCategory.getOrDefault(category.getId(), List.of());
                    long questionCount = categoryQuestions.size();
                    long activeQuestionCount = categoryQuestions.stream().filter(question -> question.getStatus() == QuestionStatus.ACTIVE).count();
                    long easyCount = categoryQuestions.stream().filter(question -> question.getStatus() == QuestionStatus.ACTIVE && question.getDifficultyLevel() == DifficultyLevel.EASY).count();
                    long mediumCount = categoryQuestions.stream().filter(question -> question.getStatus() == QuestionStatus.ACTIVE && question.getDifficultyLevel() == DifficultyLevel.MEDIUM).count();
                    long hardCount = categoryQuestions.stream().filter(question -> question.getStatus() == QuestionStatus.ACTIVE && question.getDifficultyLevel() == DifficultyLevel.HARD).count();
                    return new AdminDashboardResponse.CategoryAnalytics(
                            category.getId(),
                            category.getName(),
                            quizCountByCategory.getOrDefault(category.getId(), 0L),
                            questionCount,
                            activeQuestionCount,
                            easyCount,
                            mediumCount,
                            hardCount,
                            attemptCountByCategory.getOrDefault(category.getId(), 0L)
                    );
                })
                .sorted(Comparator.comparingLong(AdminDashboardResponse.CategoryAnalytics::attemptCount).reversed()
                        .thenComparing(Comparator.comparingLong(AdminDashboardResponse.CategoryAnalytics::questionCount).reversed()))
                .toList();

        List<AdminDashboardResponse.QuizAnalytics> quizAnalytics = quizzes.stream()
                .map(quiz -> {
                    long quizAttempts = attemptCountByQuiz.getOrDefault(quiz.getId(), 0L);
                    List<QuizAttempt> quizAttemptList = attempts.stream()
                            .filter(attempt -> attempt.getQuizVersion() != null
                                    && attempt.getQuizVersion().getQuiz() != null
                                    && quiz.getId().equals(attempt.getQuizVersion().getQuiz().getId())
                                    && attempt.getSubmittedAt() != null
                                    && attempt.getMaxScore() != null
                                    && attempt.getMaxScore() > 0)
                            .toList();
                    double avgScorePercent = quizAttemptList.stream()
                            .mapToDouble(attempt -> attempt.getScore() * 100.0 / attempt.getMaxScore())
                            .average()
                            .orElse(0.0);
                    long completed = quizAttemptList.size();
                    Integer questionCount = quiz.getCurrentVersion() == null ? 0 : quiz.getCurrentVersion().getQuestionCount();
                    return new AdminDashboardResponse.QuizAnalytics(
                            quiz.getId(),
                            quiz.getTitle(),
                            quiz.getCategory().getName(),
                            quiz.getStatus().name(),
                            questionCount,
                            quizAttempts,
                            completed,
                            avgScorePercent
                    );
                })
                .sorted(Comparator.comparingLong(AdminDashboardResponse.QuizAnalytics::attemptCount).reversed())
                .toList();

        List<AdminDashboardResponse.LeaderboardEntry> leaderboard = users.stream()
                .map(user -> {
                    List<QuizAttempt> userAttempts = attemptsByUser.getOrDefault(user.getId(), List.of());
                    if (userAttempts.isEmpty()) {
                        return new AdminDashboardResponse.LeaderboardEntry(
                                user.getId(),
                                displayName(user),
                                user.getEmail(),
                                0,
                                0,
                                0,
                                0,
                                0.0,
                                0
                        );
                    }
                    long completedCount = userAttempts.size();
                    int totalScore = userAttempts.stream().mapToInt(QuizAttempt::getScore).sum();
                    int totalMaxScore = userAttempts.stream().mapToInt(QuizAttempt::getMaxScore).sum();
                    int bestScore = userAttempts.stream().mapToInt(QuizAttempt::getScore).max().orElse(0);
                    double userAverage = totalMaxScore == 0 ? 0.0 : totalScore * 100.0 / totalMaxScore;
                    return new AdminDashboardResponse.LeaderboardEntry(
                            user.getId(),
                            displayName(user),
                            user.getEmail(),
                            userAttempts.size(),
                            completedCount,
                            totalScore,
                            totalMaxScore,
                            userAverage,
                            bestScore
                    );
                })
                .filter(entry -> entry.completedAttempts() > 0)
                .sorted(Comparator.comparingDouble(AdminDashboardResponse.LeaderboardEntry::averageScorePercent).reversed()
                        .thenComparing(Comparator.comparingInt(AdminDashboardResponse.LeaderboardEntry::totalScore).reversed())
                        .thenComparing(Comparator.comparingLong(AdminDashboardResponse.LeaderboardEntry::completedAttempts).reversed()))
                .limit(10)
                .toList();

        List<AdminDashboardResponse.RecentAttempt> recentAttempts = attempts.stream()
                .filter(attempt -> attempt.getSubmittedAt() != null)
                .sorted(Comparator.comparing(QuizAttempt::getSubmittedAt).reversed())
                .limit(8)
                .map(attempt -> new AdminDashboardResponse.RecentAttempt(
                        attempt.getId(),
                        attempt.getUser().getId(),
                        displayName(attempt.getUser()),
                        attempt.getUser().getEmail(),
                        attempt.getQuizVersion().getQuiz().getId(),
                        attempt.getQuizVersion().getTitleSnapshot(),
                        attempt.getScore(),
                        attempt.getMaxScore(),
                        attempt.getMaxScore() == null || attempt.getMaxScore() == 0 ? 0.0 : attempt.getScore() * 100.0 / attempt.getMaxScore(),
                        attempt.getSubmittedAt(),
                        attempt.getStatus().name()
                ))
                .toList();

        AdminDashboardResponse.Totals totals = new AdminDashboardResponse.Totals(
                categories.size(),
                categories.stream().filter(category -> Boolean.TRUE.equals(category.getActive())).count(),
                quizzes.size(),
                quizzes.stream().filter(quiz -> quiz.getStatus() == QuizStatus.PUBLISHED).count(),
                questions.size(),
                questions.stream().filter(question -> question.getStatus() == QuestionStatus.ACTIVE).count(),
                users.size(),
                attempts.size(),
                feedbackCount,
                completedAttempts,
                averageScorePercent
        );

        return new AdminDashboardResponse(totals, leaderboard, categoryAnalytics, quizAnalytics, recentAttempts);
    }

    private String displayName(User user) {
        if (user.getProfile() == null) {
            return user.getEmail();
        }
        String firstName = user.getProfile().getFirstName();
        String lastName = user.getProfile().getLastName();
        String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        return fullName.isBlank() ? user.getEmail() : fullName;
    }
}
