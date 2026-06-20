package com.PassFamilyDoddmane.QuizeBackend.dto.admin.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminDashboardResponse(
        Totals totals,
        List<LeaderboardEntry> leaderboard,
        List<CategoryAnalytics> categories,
        List<QuizAnalytics> quizzes,
        List<RecentAttempt> recentAttempts
) {
    public record Totals(
            long categories,
            long activeCategories,
            long quizzes,
            long publishedQuizzes,
            long questions,
            long activeQuestions,
            long users,
            long attempts,
            long feedbacks,
            long completedAttempts,
            double averageScorePercent
    ) {
    }

    public record LeaderboardEntry(
            UUID userId,
            String name,
            String email,
            long attempts,
            long completedAttempts,
            int totalScore,
            int totalMaxScore,
            double averageScorePercent,
            int bestScore
    ) {
    }

    public record CategoryAnalytics(
            UUID categoryId,
            String categoryName,
            long quizCount,
            long questionCount,
            long activeQuestionCount,
            long easyCount,
            long mediumCount,
            long hardCount,
            long attemptCount
    ) {
    }

    public record QuizAnalytics(
            UUID quizId,
            String title,
            String categoryName,
            String status,
            Integer questionCount,
            long attemptCount,
            long completedAttempts,
            double averageScorePercent
    ) {
    }

    public record RecentAttempt(
            UUID attemptId,
            UUID userId,
            String userName,
            String email,
            UUID quizId,
            String quizTitle,
            Integer score,
            Integer maxScore,
            double scorePercent,
            Instant submittedAt,
            String status
    ) {
    }
}
