package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.exception.ConflictException;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.DifficultyLevel;
import com.PassFamilyDoddmane.QuizeBackend.common.util.SlugUtil;
import com.PassFamilyDoddmane.QuizeBackend.dto.category.CategoryRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.category.CategoryResponse;
import com.PassFamilyDoddmane.QuizeBackend.entity.Question;
import com.PassFamilyDoddmane.QuizeBackend.entity.Category;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuestionRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.CategoryRepository;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.AuditAction;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final AuditService auditService;

    public CategoryResponse create(CategoryRequest request) {
        String slug = SlugUtil.toSlug(request.name());
        if (categoryRepository.existsBySlug(slug)) {
            throw new ConflictException("Category slug already exists");
        }
        Category category = new Category();
        category.setName(request.name());
        category.setSlug(slug);
        category.setDescription(request.description());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setActive(request.active() == null || request.active());
        Category saved = categoryRepository.save(category);
        auditService.logAction(AuditAction.CREATE, "Category", saved.getId().toString(), null, saved);
        return toResponse(saved, List.of());
    }

    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        String slug = SlugUtil.toSlug(request.name());
        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ConflictException("Category slug already exists");
        }
        category.setName(request.name());
        category.setSlug(slug);
        category.setDescription(request.description());
        category.setSortOrder(request.sortOrder() == null ? category.getSortOrder() : request.sortOrder());
        if (request.active() != null) {
            category.setActive(request.active());
        }
        Category saved = categoryRepository.save(category);
        auditService.logAction(AuditAction.UPDATE, "Category", saved.getId().toString(), null, saved);
        return toResponse(saved, List.of());
    }

    public void delete(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setActive(Boolean.FALSE);
        Category saved = categoryRepository.save(category);
        auditService.logAction(AuditAction.DELETE, "Category", saved.getId().toString(), null, saved);
    }

    public List<CategoryResponse> findAll() {
        List<Question> questions = questionRepository.findAll();
        Map<UUID, List<Question>> questionsByCategory = questions.stream()
                .collect(Collectors.groupingBy(question -> question.getCategory().getId()));
        return categoryRepository.findAll().stream()
                .filter(category -> Boolean.TRUE.equals(category.getActive()))
                .map(category -> toResponse(category, questionsByCategory.getOrDefault(category.getId(), List.of())))
                .toList();
    }

    private CategoryResponse toResponse(Category category, List<Question> questions) {
        long easyQuestionCount = questions.stream().filter(question -> question.getDifficultyLevel() == DifficultyLevel.EASY).count();
        long mediumQuestionCount = questions.stream().filter(question -> question.getDifficultyLevel() == DifficultyLevel.MEDIUM).count();
        long hardQuestionCount = questions.stream().filter(question -> question.getDifficultyLevel() == DifficultyLevel.HARD).count();
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getSortOrder(),
                category.getActive(),
                (long) questions.size(),
                easyQuestionCount,
                mediumQuestionCount,
                hardQuestionCount
        );
    }
}
