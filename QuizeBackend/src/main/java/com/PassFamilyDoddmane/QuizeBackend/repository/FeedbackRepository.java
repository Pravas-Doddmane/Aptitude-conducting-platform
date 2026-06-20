package com.PassFamilyDoddmane.QuizeBackend.repository;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.FeedbackStatus;
import com.PassFamilyDoddmane.QuizeBackend.entity.Feedback;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    @EntityGraph(attributePaths = {"user", "user.profile", "quiz", "question", "reviewedBy", "reviewedBy.profile"})
    List<Feedback> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user", "user.profile", "quiz", "question", "reviewedBy", "reviewedBy.profile"})
    List<Feedback> findByStatusOrderByCreatedAtDesc(FeedbackStatus status);
}
