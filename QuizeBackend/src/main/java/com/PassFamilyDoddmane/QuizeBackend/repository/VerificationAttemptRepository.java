package com.PassFamilyDoddmane.QuizeBackend.repository;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.VerificationResult;
import com.PassFamilyDoddmane.QuizeBackend.entity.VerificationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface VerificationAttemptRepository extends JpaRepository<VerificationAttempt, UUID> {
    
    List<VerificationAttempt> findByIdentityVerificationIdOrderByVerificationTimestampAsc(UUID identityVerificationId);
    
    List<VerificationAttempt> findByIdentityVerificationIdAndVerificationResult(
            UUID identityVerificationId,
            VerificationResult result
    );
    
    @Query("SELECT va FROM VerificationAttempt va WHERE va.identityVerification.id = :verificationId " +
           "AND va.verificationTimestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY va.verificationTimestamp ASC")
    List<VerificationAttempt> findByVerificationIdAndTimeRange(
            @Param("verificationId") UUID verificationId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
    
    Long countByIdentityVerificationIdAndVerificationResult(UUID identityVerificationId, VerificationResult result);
    
    @Query("SELECT AVG(va.matchScore) FROM VerificationAttempt va WHERE va.identityVerification.id = :verificationId")
    Double calculateAverageMatchScore(@Param("verificationId") UUID verificationId);
    
    List<VerificationAttempt> findByTriggeredViolationTrue();
}
