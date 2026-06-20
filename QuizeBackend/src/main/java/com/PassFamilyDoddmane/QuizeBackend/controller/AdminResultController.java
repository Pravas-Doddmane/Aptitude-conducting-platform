package com.PassFamilyDoddmane.QuizeBackend.controller;

import com.PassFamilyDoddmane.QuizeBackend.dto.admin.result.AdminQuizParticipantResultResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.admin.result.AdminQuizResultSummaryResponse;
import com.PassFamilyDoddmane.QuizeBackend.service.AdminResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/results")
@RequiredArgsConstructor
public class AdminResultController {

    private final AdminResultService adminResultService;

    @GetMapping
    public ResponseEntity<List<AdminQuizResultSummaryResponse>> summaries() {
        return ResponseEntity.ok(adminResultService.summaries());
    }

    @GetMapping("/{quizId}/participants")
    public ResponseEntity<List<AdminQuizParticipantResultResponse>> participants(
            @PathVariable UUID quizId,
            @RequestParam(defaultValue = "ALL") String filter
    ) {
        return ResponseEntity.ok(adminResultService.participantResults(quizId, filter));
    }
}
