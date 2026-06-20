package com.PassFamilyDoddmane.QuizeBackend.repository;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.VerificationStatus;
import com.PassFamilyDoddmane.QuizeBackend.entity.IdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification, UUID> {
    
    Optional<IdentityVerification> findByProctoringSessionId(UUID proctoringSessionId);
    
    List<IdentityVerification> findByUserId(UUID userId);
    
    List<IdentityVerification> findByVerificationStatus(VerificationStatus status);
    
    @Query("SELECT iv FROM IdentityVerification iv WHERE iv.proctoringSession.quizAttempt.id = :attemptId")
    Optional<IdentityVerification> findByAttemptId(@Param("attemptId") UUID attemptId);
    
    @Query("SELECT iv FROM IdentityVerification iv WHERE iv.proctoringSession.quizAttempt.quizVersion.quiz.id = :quizId")
    List<IdentityVerification> findByQuizId(@Param("quizId") UUID quizId);
    
    @Query("SELECT COUNT(iv) FROM IdentityVerification iv WHERE iv.user.id = :userId AND iv.verificationStatus = :status")
    Long countByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") VerificationStatus status);
}
