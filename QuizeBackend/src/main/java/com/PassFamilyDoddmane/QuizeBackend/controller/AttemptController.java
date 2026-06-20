package com.PassFamilyDoddmane.QuizeBackend.controller;

import com.PassFamilyDoddmane.QuizeBackend.dto.attempt.AttemptResultResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.attempt.AttemptSummaryResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.attempt.StartAttemptResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.attempt.SubmitAttemptRequest;
import com.PassFamilyDoddmane.QuizeBackend.service.AttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attempts")
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;

    @PostMapping("/start/{quizId}")
    public ResponseEntity<StartAttemptResponse> start(@PathVariable UUID quizId) {
        return ResponseEntity.ok(attemptService.startAttempt(quizId));
    }

    @PostMapping("/{attemptId}/submit")
    public ResponseEntity<AttemptResultResponse> submit(@PathVariable UUID attemptId, @Valid @RequestBody SubmitAttemptRequest request) {
        return ResponseEntity.ok(attemptService.submitAttempt(attemptId, request));
    }

    @PostMapping("/{attemptId}/auto-submit")
    public ResponseEntity<AttemptResultResponse> autoSubmit(
            @PathVariable UUID attemptId,
            @RequestBody(required = false) SubmitAttemptRequest request
    ) {
        return ResponseEntity.ok(attemptService.autoSubmit(attemptId, request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<AttemptSummaryResponse>> myAttempts() {
        return ResponseEntity.ok(attemptService.myAttempts());
    }

    @GetMapping("/{attemptId}")
    public ResponseEntity<AttemptResultResponse> result(@PathVariable UUID attemptId) {
        return ResponseEntity.ok(attemptService.getResult(attemptId));
    }
}
