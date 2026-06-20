package com.PassFamilyDoddmane.QuizeBackend.controller;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.DifficultyLevel;
import com.PassFamilyDoddmane.QuizeBackend.dto.ai.AiQuestionGenerateRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.ai.AiQuestionGenerateResponse;
import com.PassFamilyDoddmane.QuizeBackend.service.AiQuestionGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/ai/questions")
@RequiredArgsConstructor
public class AdminAiQuestionController {

    private final AiQuestionGenerationService aiQuestionGenerationService;

    @PostMapping("/generate")
    public ResponseEntity<AiQuestionGenerateResponse> generate(@Valid @RequestBody AiQuestionGenerateRequest request) {
        return ResponseEntity.ok(aiQuestionGenerationService.generate(request));
    }

    @PostMapping(value = "/generate-from-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiQuestionGenerateResponse> generateFromFile(
            @RequestParam UUID categoryId,
            @RequestParam String topic,
            @RequestParam DifficultyLevel difficultyLevel,
            @RequestParam Integer questionCount,
            @RequestParam Integer optionsPerQuestion,
            @RequestParam MultipartFile file
    ) {
        AiQuestionGenerateRequest request = new AiQuestionGenerateRequest(
                categoryId,
                topic,
                difficultyLevel,
                questionCount,
                optionsPerQuestion
        );
        return ResponseEntity.ok(aiQuestionGenerationService.generateFromFile(request, file));
    }
}
