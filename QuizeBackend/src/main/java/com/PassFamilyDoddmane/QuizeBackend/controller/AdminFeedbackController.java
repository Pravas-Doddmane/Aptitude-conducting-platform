package com.PassFamilyDoddmane.QuizeBackend.controller;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.FeedbackStatus;
import com.PassFamilyDoddmane.QuizeBackend.dto.feedback.FeedbackResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.feedback.FeedbackStatusUpdateRequest;
import com.PassFamilyDoddmane.QuizeBackend.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    public ResponseEntity<List<FeedbackResponse>> all(@RequestParam(required = false) FeedbackStatus status) {
        return ResponseEntity.ok(feedbackService.findAll(status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FeedbackResponse> updateStatus(@PathVariable UUID id, @Valid @RequestBody FeedbackStatusUpdateRequest request) {
        return ResponseEntity.ok(feedbackService.updateStatus(id, request));
    }
}
