package com.PassFamilyDoddmane.QuizeBackend.repository;

import com.PassFamilyDoddmane.QuizeBackend.entity.QuizVersionQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizVersionQuestionRepository extends JpaRepository<QuizVersionQuestion, UUID> {
    List<QuizVersionQuestion> findByQuizVersionIdOrderByDisplayOrderAsc(UUID quizVersionId);
    void deleteByQuizVersionId(UUID quizVersionId);
}
