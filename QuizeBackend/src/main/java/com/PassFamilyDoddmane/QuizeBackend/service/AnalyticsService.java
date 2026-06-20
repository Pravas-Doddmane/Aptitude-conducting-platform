package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.dto.stats.UserStatsResponse;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizAttemptRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class AnalyticsService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final CurrentUserService currentUserService;

    public UserStatsResponse myStats() {
        var user = currentUserService.getCurrentUser();
        var attempts = quizAttemptRepository.findByUserIdOrderByStartedAtDesc(user.getId());
        long completedAttempts = attempts.stream().filter(attempt -> attempt.getSubmittedAt() != null).count();
        double averageScore = attempts.stream().mapToInt(attempt -> attempt.getScore() == null ? 0 : attempt.getScore()).average().orElse(0);
        Integer bestScore = attempts.stream().map(attempt -> attempt.getScore() == null ? 0 : attempt.getScore()).max(Integer::compareTo).orElse(0);
        return new UserStatsResponse(attempts.size(), averageScore, bestScore, completedAttempts);
    }
}
