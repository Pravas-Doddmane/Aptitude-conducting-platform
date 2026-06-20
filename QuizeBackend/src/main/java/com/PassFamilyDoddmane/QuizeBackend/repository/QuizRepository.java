package com.PassFamilyDoddmane.QuizeBackend.repository;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.QuizStatus;
import com.PassFamilyDoddmane.QuizeBackend.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    @Query("""
            select distinct quiz from Quiz quiz
            left join quiz.categories category
            where quiz.category.id = :categoryId or category.id = :categoryId
            """)
    List<Quiz> findByCategoryId(@Param("categoryId") UUID categoryId);
    List<Quiz> findByStatus(QuizStatus status);
    Optional<Quiz> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
}
