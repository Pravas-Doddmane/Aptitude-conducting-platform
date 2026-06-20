package com.PassFamilyDoddmane.QuizeBackend.dto.proctoring;

import java.time.Instant;
import java.util.UUID;

public record ProctoringViolationResponse(
        UUID violationId,
        UUID sessionId,
        String violationType,
        Instant violationTimestamp,
        String description,
        String snapshotUrl,
        String evidenceUrl,
        Integer livesDeducted,
        Integer remainingLivesAfter,
        Boolean autoFlagged,
        Boolean reviewed,
        Instant reviewedAt,
        String reviewerNotes
) {
}
