package com.PassFamilyDoddmane.QuizeBackend.controller;

import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.DetailedProctoringReport;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.ProctoringSessionResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.proctoring.ProctoringViolationResponse;
import com.PassFamilyDoddmane.QuizeBackend.service.AdminProctoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/proctoring")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProctoringController {

    private final AdminProctoringService adminProctoringService;

    /**
     * Get detailed proctoring report for an attempt
     * GET /api/admin/proctoring/attempts/{attemptId}/report
     */
    @GetMapping("/attempts/{attemptId}/report")
    public ResponseEntity<DetailedProctoringReport> getDetailedReport(@PathVariable UUID attemptId) {
        return ResponseEntity.ok(adminProctoringService.getDetailedReport(attemptId));
    }

    /**
     * Get all proctoring sessions for a quiz
     * GET /api/admin/proctoring/quizzes/{quizId}/sessions
     */
    @GetMapping("/quizzes/{quizId}/sessions")
    public ResponseEntity<List<ProctoringSessionResponse>> getSessionsByQuiz(@PathVariable UUID quizId) {
        return ResponseEntity.ok(adminProctoringService.getSessionsByQuiz(quizId));
    }

    /**
     * Get all proctoring sessions for a user
     * GET /api/admin/proctoring/users/{userId}/sessions
     */
    @GetMapping("/users/{userId}/sessions")
    public ResponseEntity<List<ProctoringSessionResponse>> getSessionsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminProctoringService.getSessionsByUser(userId));
    }

    /**
     * Get unreviewed violations
     * GET /api/admin/proctoring/violations/unreviewed
     */
    @GetMapping("/violations/unreviewed")
    public ResponseEntity<List<ProctoringViolationResponse>> getUnreviewedViolations() {
        return ResponseEntity.ok(adminProctoringService.getUnreviewedViolations());
    }

    /**
     * Mark violation as reviewed
     * PATCH /api/admin/proctoring/violations/{violationId}/review
     */
    @PatchMapping("/violations/{violationId}/review")
    public ResponseEntity<ProctoringViolationResponse> markViolationReviewed(
            @PathVariable UUID violationId,
            @RequestParam(required = false) String reviewerNotes) {
        return ResponseEntity.ok(adminProctoringService.markViolationReviewed(violationId, reviewerNotes));
    }

    /**
     * Get all violations for a quiz
     * GET /api/admin/proctoring/quizzes/{quizId}/violations
     */
    @GetMapping("/quizzes/{quizId}/violations")
    public ResponseEntity<List<ProctoringViolationResponse>> getViolationsByQuiz(@PathVariable UUID quizId) {
        return ResponseEntity.ok(adminProctoringService.getViolationsByQuiz(quizId));
    }
}
