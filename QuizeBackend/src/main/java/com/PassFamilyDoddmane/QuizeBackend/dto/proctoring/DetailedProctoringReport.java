package com.PassFamilyDoddmane.QuizeBackend.dto.proctoring;

import java.util.List;
import java.util.UUID;

public record DetailedProctoringReport(
        UUID attemptId,
        String userEmail,
        String userName,
        String quizTitle,
        ProctoringSessionResponse session,
        List<ProctoringViolationResponse> violations,
        List<ProctoringEventResponse> events,
        String autoSubmitReason,
        Boolean wasAutoSubmitted
) {
}
