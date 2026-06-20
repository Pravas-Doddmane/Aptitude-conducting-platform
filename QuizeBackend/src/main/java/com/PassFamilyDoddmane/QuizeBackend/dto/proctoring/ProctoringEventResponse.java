package com.PassFamilyDoddmane.QuizeBackend.dto.proctoring;

import java.time.Instant;
import java.util.UUID;

public record ProctoringEventResponse(
        UUID eventId,
        UUID sessionId,
        String eventType,
        String severity,
        Instant eventTimestamp,
        String description,
        String frameDataUrl,
        Double confidenceScore,
        Integer livesDeducted,
        Integer remainingLivesSnapshot
) {
}
