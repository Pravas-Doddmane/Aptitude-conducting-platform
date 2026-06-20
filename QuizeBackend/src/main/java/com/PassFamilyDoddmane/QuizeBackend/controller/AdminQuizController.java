package com.PassFamilyDoddmane.QuizeBackend.controller;

import com.PassFamilyDoddmane.QuizeBackend.dto.quiz.QuizRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.quiz.QuizResponse;
import com.PassFamilyDoddmane.QuizeBackend.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/quizzes")
@RequiredArgsConstructor
public class AdminQuizController {

    private final QuizService quizService;

    @PostMapping
    public ResponseEntity<QuizResponse> create(@Valid @RequestBody QuizRequest request) {
        return ResponseEntity.ok(quizService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuizResponse> update(@PathVariable UUID id, @Valid @RequestBody QuizRequest request) {
        return ResponseEntity.ok(quizService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        quizService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<QuizResponse> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(quizService.publish(id));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<QuizResponse> unpublish(@PathVariable UUID id) {
        return ResponseEntity.ok(quizService.unpublish(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(quizService.getForAdmin(id));
    }

    @GetMapping
    public ResponseEntity<List<QuizResponse>> allByCategory(@RequestParam(required = false) UUID categoryId) {
        return ResponseEntity.ok(categoryId == null ? quizService.findAllForAdmin() : quizService.findByCategoryForAdmin(categoryId));
    }
}
