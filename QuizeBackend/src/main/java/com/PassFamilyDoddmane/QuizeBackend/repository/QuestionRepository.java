package com.PassFamilyDoddmane.QuizeBackend.repository;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.QuestionStatus;
import com.PassFamilyDoddmane.QuizeBackend.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findByCategoryId(UUID categoryId);
    List<Question> findByCategoryIdAndStatus(UUID categoryId, QuestionStatus status);
    List<Question> findByStatus(QuestionStatus status);
}
