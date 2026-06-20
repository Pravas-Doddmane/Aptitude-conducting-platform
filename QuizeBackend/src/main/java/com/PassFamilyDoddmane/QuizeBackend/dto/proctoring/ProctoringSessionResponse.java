package com.PassFamilyDoddmane.QuizeBackend.dto.proctoring;

import java.time.Instant;
import java.util.UUID;

public record ProctoringSessionResponse(
        UUID sessionId,
        UUID attemptId,
        String status,
        Instant startedAt,
        Instant endedAt,
        Integer initialLives,
        Integer remainingLives,
        Integer totalViolations,
        Integer criticalViolations,
        Integer warningViolations,
        Integer riskScore,
        String riskLevel,
        Boolean proctoringEnabled,
        String notes
) {
}
