package com.PassFamilyDoddmane.QuizeBackend.controller;

import com.PassFamilyDoddmane.QuizeBackend.dto.category.CategoryResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.quiz.QuizResponse;
import com.PassFamilyDoddmane.QuizeBackend.service.CategoryService;
import com.PassFamilyDoddmane.QuizeBackend.service.QuizService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuizController {

    private final CategoryService categoryService;
    private final QuizService quizService;

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> categories() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/quizzes")
    public ResponseEntity<List<QuizResponse>> quizzes(@RequestParam UUID categoryId) {
        return ResponseEntity.ok(quizService.findByCategory(categoryId));
    }

    @GetMapping("/quizzes/{id}")
    public ResponseEntity<QuizResponse> quiz(@PathVariable UUID id) {
        return ResponseEntity.ok(quizService.get(id));
    }
}
