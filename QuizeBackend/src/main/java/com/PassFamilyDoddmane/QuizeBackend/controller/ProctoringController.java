package com.PassFamilyDoddmane.QuizeBackend.controller;

import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.ProctoringEventResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.ProctoringSessionResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.ProctoringViolationResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.RecordEventRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.RecordViolationRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.ViolationTimelineResponse;
import com.PassFamilyDoddmane.QuizeBackend.service.ProctoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/proctoring")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class ProctoringController {

    private final ProctoringService proctoringService;

    /**
     * Initialize proctoring session for an attempt
     * POST /api/proctoring/attempts/{attemptId}/session
     */
    @PostMapping("/attempts/{attemptId}/session")
    public ResponseEntity<ProctoringSessionResponse> initializeSession(@PathVariable UUID attemptId) {
        return ResponseEntity.ok(proctoringService.initializeProctoringSession(attemptId));
    }

    /**
     * Get proctoring session details
     * GET /api/proctoring/attempts/{attemptId}/session
     */
    @GetMapping("/attempts/{attemptId}/session")
    public ResponseEntity<ProctoringSessionResponse> getSession(@PathVariable UUID attemptId) {
        return ResponseEntity.ok(proctoringService.getSession(attemptId));
    }

    /**
     * End proctoring session
     * POST /api/proctoring/attempts/{attemptId}/session/end
     */
    @PostMapping("/attempts/{attemptId}/session/end")
    public ResponseEntity<ProctoringSessionResponse> endSession(@PathVariable UUID attemptId) {
        return ResponseEntity.ok(proctoringService.endSession(attemptId));
    }

    /**
     * Record a proctoring event
     * POST /api/proctoring/attempts/{attemptId}/events
     */
    @PostMapping("/attempts/{attemptId}/events")
    public ResponseEntity<ProctoringEventResponse> recordEvent(
            @PathVariable UUID attemptId,
            @Valid @RequestBody RecordEventRequest request) {
        return ResponseEntity.ok(proctoringService.recordEvent(attemptId, request));
    }

    /**
     * Record a violation
     * POST /api/proctoring/attempts/{attemptId}/violations
     */
    @PostMapping("/attempts/{attemptId}/violations")
    public ResponseEntity<ProctoringViolationResponse> recordViolation(
            @PathVariable UUID attemptId,
            @Valid @RequestBody RecordViolationRequest request) {
        return ResponseEntity.ok(proctoringService.recordViolation(attemptId, request));
    }

    /**
     * Get violation timeline
     * GET /api/proctoring/attempts/{attemptId}/violations/timeline
     */
    @GetMapping("/attempts/{attemptId}/violations/timeline")
    public ResponseEntity<List<ViolationTimelineResponse>> getViolationTimeline(@PathVariable UUID attemptId) {
        return ResponseEntity.ok(proctoringService.getViolationTimeline(attemptId));
    }
}
