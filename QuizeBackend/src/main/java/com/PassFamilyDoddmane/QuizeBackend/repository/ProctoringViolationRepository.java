package com.PassFamilyDoddmane.QuizeBackend.repository;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.ViolationType;
import com.PassFamilyDoddmane.QuizeBackend.entity.ProctoringViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProctoringViolationRepository extends JpaRepository<ProctoringViolation, UUID> {
    
    List<ProctoringViolation> findByProctoringSessionIdOrderByViolationTimestampAsc(UUID proctoringSessionId);
    
    List<ProctoringViolation> findByProctoringSessionIdAndViolationTypeOrderByViolationTimestampAsc(
            UUID proctoringSessionId, 
            ViolationType violationType
    );
    
    Long countByProctoringSessionId(UUID proctoringSessionId);
    
    Long countByProctoringSessionIdAndViolationType(UUID proctoringSessionId, ViolationType violationType);
    
    @Query("SELECT pv FROM ProctoringViolation pv WHERE pv.proctoringSession.id = :sessionId " +
           "AND pv.violationTimestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY pv.violationTimestamp ASC")
    List<ProctoringViolation> findBySessionIdAndTimeRange(
            @Param("sessionId") UUID sessionId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
    
    List<ProctoringViolation> findByReviewedFalseOrderByViolationTimestampDesc();
    
    @Query("SELECT pv FROM ProctoringViolation pv WHERE pv.proctoringSession.quizAttempt.quizVersion.quiz.id = :quizId " +
           "ORDER BY pv.violationTimestamp DESC")
    List<ProctoringViolation> findByQuizId(@Param("quizId") UUID quizId);
}
