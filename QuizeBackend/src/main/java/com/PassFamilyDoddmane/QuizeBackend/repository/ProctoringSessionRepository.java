package com.PassFamilyDoddmane.QuizeBackend.repository;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.ProctoringSessionStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.RiskLevel;
import com.PassFamilyDoddmane.QuizeBackend.entity.ProctoringSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProctoringSessionRepository extends JpaRepository<ProctoringSession, UUID> {
    
    Optional<ProctoringSession> findByQuizAttemptId(UUID quizAttemptId);
    
    List<ProctoringSession> findByStatus(ProctoringSessionStatus status);
    
    List<ProctoringSession> findByRiskLevel(RiskLevel riskLevel);
    
    @Query("SELECT ps FROM ProctoringSession ps WHERE ps.quizAttempt.user.id = :userId ORDER BY ps.startedAt DESC")
    List<ProctoringSession> findByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT ps FROM ProctoringSession ps WHERE ps.quizAttempt.quizVersion.quiz.id = :quizId ORDER BY ps.startedAt DESC")
    List<ProctoringSession> findByQuizId(@Param("quizId") UUID quizId);
    
    @Query("SELECT COUNT(ps) FROM ProctoringSession ps WHERE ps.quizAttempt.user.id = :userId AND ps.riskLevel = :riskLevel")
    Long countByUserIdAndRiskLevel(@Param("userId") UUID userId, @Param("riskLevel") RiskLevel riskLevel);
}
