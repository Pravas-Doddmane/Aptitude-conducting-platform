package com.PassFamilyDoddmane.QuizeBackend.repository;

import com.PassFamilyDoddmane.QuizeBackend.entity.QuizVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizVersionRepository extends JpaRepository<QuizVersion, UUID> {
    List<QuizVersion> findByQuizIdOrderByVersionNoDesc(UUID quizId);
    Optional<QuizVersion> findTopByQuizIdOrderByVersionNoDesc(UUID quizId);
}
