package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.QuizStatus;
import com.PassFamilyDoddmane.QuizeBackend.entity.Quiz;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class QuizAvailabilityScheduler {

    private final QuizRepository quizRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void closeExpiredQuizzes() {
        Instant now = Instant.now();
        for (Quiz quiz : quizRepository.findAll()) {
            if (quiz.getStatus() == QuizStatus.PUBLISHED
                    && quiz.getAvailableTo() != null
                    && quiz.getAvailableTo().isBefore(now)) {
                quiz.setStatus(QuizStatus.UNPUBLISHED);
                quizRepository.save(quiz);
            }
        }
    }
}
