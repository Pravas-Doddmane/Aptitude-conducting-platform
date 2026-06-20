package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.QuestionStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.AuditAction;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.DifficultyLevel;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.BadRequestException;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.dto.question.QuestionOptionRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.question.QuestionOptionResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.question.QuestionRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.question.QuestionResponse;
import com.PassFamilyDoddmane.QuizeBackend.entity.Category;
import com.PassFamilyDoddmane.QuizeBackend.entity.Question;
import com.PassFamilyDoddmane.QuizeBackend.entity.QuestionOption;
import com.PassFamilyDoddmane.QuizeBackend.repository.CategoryRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuestionOptionRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuestionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public QuestionResponse create(QuestionRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        validateOptions(request.options());
        Question question = new Question();
        question.setCategory(category);
        question.setCreatedBy(currentUserService.getCurrentUser());
        question.setStem(request.stem());
        question.setExplanation(request.explanation());
        question.setImageUrl(request.imageUrl());
        question.setDifficultyLevel(request.difficultyLevel());
        int difficultyWeight = difficultyWeight(request.difficultyLevel());
        question = questionRepository.save(question);
        saveOptions(question, request.options());
        QuestionResponse response = toResponse(question);
        auditService.logAction(
                AuditAction.CREATE,
                "Question",
                question.getId().toString(),
                null,
                java.util.Map.of("question", response, "difficultyWeight", difficultyWeight)
        );
        return response;
    }

    public QuestionResponse update(UUID id, QuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        validateOptions(request.options());
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        question.setCategory(category);
        question.setStem(request.stem());
        question.setExplanation(request.explanation());
        question.setImageUrl(request.imageUrl());
        question.setDifficultyLevel(request.difficultyLevel());
        int difficultyWeight = difficultyWeight(request.difficultyLevel());
        questionRepository.save(question);
        questionOptionRepository.deleteAll(questionOptionRepository.findByQuestionIdOrderByOptionOrderAsc(question.getId()));
        saveOptions(question, request.options());
        QuestionResponse response = toResponse(question);
        auditService.logAction(
                AuditAction.UPDATE,
                "Question",
                question.getId().toString(),
                null,
                java.util.Map.of("question", response, "difficultyWeight", difficultyWeight)
        );
        return response;
    }

    public void delete(UUID id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        question.setStatus(QuestionStatus.INACTIVE);
        Question saved = questionRepository.save(question);
        auditService.logAction(AuditAction.DELETE, "Question", saved.getId().toString(), null, saved);
    }

    public List<QuestionResponse> findByCategory(UUID categoryId) {
        return questionRepository.findByCategoryIdAndStatus(categoryId, QuestionStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    public List<QuestionResponse> findAll() {
        return questionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public QuestionResponse get(UUID id) {
        return toResponse(questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found")));
    }

    private void saveOptions(Question question, List<QuestionOptionRequest> optionRequests) {
        List<QuestionOption> options = optionRequests.stream()
                .sorted(Comparator.comparingInt(QuestionOptionRequest::optionOrder))
                .map(optionRequest -> {
                    QuestionOption option = new QuestionOption();
                    option.setQuestion(question);
                    option.setOptionOrder(optionRequest.optionOrder());
                    option.setOptionText(optionRequest.optionText());
                    option.setCorrect(optionRequest.correct());
                    return option;
                }).toList();
        questionOptionRepository.saveAll(options);
    }

    private void validateOptions(List<QuestionOptionRequest> options) {
        if (options == null || options.size() < 2 || options.size() > 6) {
            throw new BadRequestException("Each question must have between 2 and 6 options");
        }
        long correctCount = options.stream().filter(QuestionOptionRequest::correct).count();
        if (correctCount != 1) {
            throw new BadRequestException("Exactly one option must be marked as correct");
        }
    }

    private int difficultyWeight(DifficultyLevel difficultyLevel) {
        return switch (difficultyLevel) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 3;
        };
    }

    private QuestionResponse toResponse(Question question) {
        List<QuestionOptionResponse> options = questionOptionRepository.findByQuestionIdOrderByOptionOrderAsc(question.getId())
                .stream()
                .map(option -> new QuestionOptionResponse(
                        option.getId(),
                        option.getOptionOrder(),
                        option.getOptionText(),
                        option.getCorrect()
                ))
                .toList();
        return new QuestionResponse(
                question.getId(),
                question.getCategory().getId(),
                question.getStem(),
                question.getExplanation(),
                question.getImageUrl(),
                question.getDifficultyLevel(),
                question.getStatus().name(),
                options
        );
    }
}
