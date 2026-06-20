package com.PassFamilyDoddmane.QuizeBackend.controller;

import com.PassFamilyDoddmane.QuizeBackend.dto.identity.CaptureReferenceImageRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.identity.IdentityVerificationResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.identity.RecordVerificationAttemptRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.identity.VerificationAttemptResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.identity.VerificationTimelineResponse;
import com.PassFamilyDoddmane.QuizeBackend.service.IdentityVerificationService;
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
@RequestMapping("/api/identity-verification")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class IdentityVerificationController {

    private final IdentityVerificationService identityVerificationService;

    /**
     * Capture reference image for identity verification
     * POST /api/identity-verification/attempts/{attemptId}/reference
     */
    @PostMapping("/attempts/{attemptId}/reference")
    public ResponseEntity<IdentityVerificationResponse> captureReferenceImage(
            @PathVariable UUID attemptId,
            @Valid @RequestBody CaptureReferenceImageRequest request) {
        return ResponseEntity.ok(identityVerificationService.captureReferenceImage(attemptId, request));
    }

    /**
     * Record a verification attempt (face comparison result)
     * POST /api/identity-verification/attempts/{attemptId}/verify
     */
    @PostMapping("/attempts/{attemptId}/verify")
    public ResponseEntity<VerificationAttemptResponse> recordVerificationAttempt(
            @PathVariable UUID attemptId,
            @Valid @RequestBody RecordVerificationAttemptRequest request) {
        return ResponseEntity.ok(identityVerificationService.recordVerificationAttempt(attemptId, request));
    }

    /**
     * Get identity verification details
     * GET /api/identity-verification/attempts/{attemptId}
     */
    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<IdentityVerificationResponse> getVerification(@PathVariable UUID attemptId) {
        return ResponseEntity.ok(identityVerificationService.getVerification(attemptId));
    }

    /**
     * Get verification timeline
     * GET /api/identity-verification/attempts/{attemptId}/timeline
     */
    @GetMapping("/attempts/{attemptId}/timeline")
    public ResponseEntity<List<VerificationTimelineResponse>> getVerificationTimeline(@PathVariable UUID attemptId) {
        return ResponseEntity.ok(identityVerificationService.getVerificationTimeline(attemptId));
    }
}
