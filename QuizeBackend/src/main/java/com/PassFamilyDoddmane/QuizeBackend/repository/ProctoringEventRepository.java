package com.PassFamilyDoddmane.QuizeBackend.repository;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.EventSeverity;
import com.PassFamilyDoddmane.QuizeBackend.entity.ProctoringEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProctoringEventRepository extends JpaRepository<ProctoringEvent, UUID> {
    
    List<ProctoringEvent> findByProctoringSessionIdOrderByEventTimestampAsc(UUID proctoringSessionId);
    
    List<ProctoringEvent> findByProctoringSessionIdAndSeverityOrderByEventTimestampAsc(
            UUID proctoringSessionId, 
            EventSeverity severity
    );
    
    @Query("SELECT pe FROM ProctoringEvent pe WHERE pe.proctoringSession.id = :sessionId " +
           "AND pe.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY pe.eventTimestamp ASC")
    List<ProctoringEvent> findBySessionIdAndTimeRange(
            @Param("sessionId") UUID sessionId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
    
    Long countByProctoringSessionId(UUID proctoringSessionId);
    
    Long countByProctoringSessionIdAndSeverity(UUID proctoringSessionId, EventSeverity severity);
}
