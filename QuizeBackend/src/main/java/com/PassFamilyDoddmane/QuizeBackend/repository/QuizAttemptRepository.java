package com.PassFamilyDoddmane.QuizeBackend.repository;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.AttemptStatus;
import com.PassFamilyDoddmane.QuizeBackend.entity.QuizAttempt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
    Optional<QuizAttempt> findByIdAndUserId(UUID id, UUID userId);
    List<QuizAttempt> findByUserIdOrderByStartedAtDesc(UUID userId);
    long countByUserIdAndQuizVersionQuizId(UUID userId, UUID quizId);
    Optional<QuizAttempt> findTopByUserIdAndQuizVersionQuizIdOrderByAttemptNumberDesc(UUID userId, UUID quizId);
    List<QuizAttempt> findByStatus(AttemptStatus status);

    @EntityGraph(attributePaths = {"user", "user.profile", "quizVersion", "quizVersion.quiz", "quizVersion.quiz.category"})
    @Query("select qa from QuizAttempt qa")
    List<QuizAttempt> findAllWithRelations();
}
