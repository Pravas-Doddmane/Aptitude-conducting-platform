package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.QuizStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.AuditAction;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.DifficultyLevel;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.QuestionStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.BadRequestException;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.common.util.SlugUtil;
import com.PassFamilyDoddmane.QuizeBackend.dto.quiz.QuizRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.quiz.QuizResponse;
import com.PassFamilyDoddmane.QuizeBackend.entity.Category;
import com.PassFamilyDoddmane.QuizeBackend.entity.CertificateTemplate;
import com.PassFamilyDoddmane.QuizeBackend.entity.Question;
import com.PassFamilyDoddmane.QuizeBackend.entity.Quiz;
import com.PassFamilyDoddmane.QuizeBackend.entity.QuizVersion;
import com.PassFamilyDoddmane.QuizeBackend.entity.QuizVersionQuestion;
import com.PassFamilyDoddmane.QuizeBackend.repository.CategoryRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.CertificateTemplateRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuestionRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizVersionQuestionRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizVersionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizVersionRepository quizVersionRepository;
    private final QuizVersionQuestionRepository quizVersionQuestionRepository;
    private final CategoryRepository categoryRepository;
    private final CertificateTemplateRepository certificateTemplateRepository;
    private final QuestionRepository questionRepository;
    private final AuditService auditService;

    public QuizResponse create(QuizRequest request) {
        validateAvailabilityWindow(request.availableFrom(), request.availableTo());
        List<Category> categories = resolveCategories(request);
        Quiz quiz = new Quiz();
        applyCategories(quiz, categories);
        quiz.setTitle(request.title());
        quiz.setSlug(uniqueSlug(request.title()));
        quiz.setDescription(request.description());
        quiz.setDurationSeconds(request.durationSeconds());
        quiz.setMaxAttemptsPerUser(request.maxAttemptsPerUser() == null ? 0 : request.maxAttemptsPerUser());
        quiz.setAvailableFrom(request.availableFrom());
        quiz.setAvailableTo(request.availableTo());
        applyQuizMode(quiz, request);
        applyCertificateSettings(quiz, request);
        quiz.setStatus(QuizStatus.DRAFT);
        quiz = quizRepository.save(quiz);
        createVersion(quiz, request, 1);
        QuizResponse response = toResponse(quizRepository.save(quiz));
        auditService.logAction(AuditAction.CREATE, "Quiz", quiz.getId().toString(), null, response);
        return response;
    }

    public QuizResponse update(UUID id, QuizRequest request) {
        validateAvailabilityWindow(request.availableFrom(), request.availableTo());
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        List<Category> categories = resolveCategories(request);
        String slug = uniqueSlug(request.title(), id);
        applyCategories(quiz, categories);
        quiz.setTitle(request.title());
        quiz.setDescription(request.description());
        quiz.setDurationSeconds(request.durationSeconds());
        quiz.setMaxAttemptsPerUser(request.maxAttemptsPerUser() == null ? 0 : request.maxAttemptsPerUser());
        quiz.setAvailableFrom(request.availableFrom());
        quiz.setAvailableTo(request.availableTo());
        applyQuizMode(quiz, request);
        applyCertificateSettings(quiz, request);
        quiz.setSlug(slug);
        Integer nextVersion = quizVersionRepository.findTopByQuizIdOrderByVersionNoDesc(id)
                .map(version -> version.getVersionNo() + 1)
                .orElse(1);
        createVersion(quiz, request, nextVersion);
        QuizResponse response = toResponse(quizRepository.save(quiz));
        auditService.logAction(AuditAction.UPDATE, "Quiz", quiz.getId().toString(), null, response);
        return response;
    }

    public void delete(UUID id) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        quiz.setStatus(QuizStatus.ARCHIVED);
        Quiz saved = quizRepository.save(quiz);
        auditService.logAction(AuditAction.DELETE, "Quiz", saved.getId().toString(), null, saved);
    }

    public QuizResponse publish(UUID id) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        QuizVersion version = quizVersionRepository.findTopByQuizIdOrderByVersionNoDesc(id)
                .orElseThrow(() -> new BadRequestException("Quiz version not found"));
        version.setStatus(QuizStatus.PUBLISHED);
        version.setPublishedAt(Instant.now());
        quiz.setCurrentVersion(version);
        quiz.setStatus(QuizStatus.PUBLISHED);
        quizVersionRepository.save(version);
        QuizResponse response = toResponse(quizRepository.save(quiz));
        auditService.logAction(AuditAction.PUBLISH, "Quiz", quiz.getId().toString(), null, response);
        return response;
    }

    public QuizResponse unpublish(UUID id) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        quiz.setStatus(QuizStatus.UNPUBLISHED);
        Quiz saved = quizRepository.save(quiz);
        QuizResponse response = toResponse(saved);
        auditService.logAction(AuditAction.UNPUBLISH, "Quiz", quiz.getId().toString(), null, response);
        return response;
    }

    public List<QuizResponse> findByCategory(UUID categoryId) {
        List<Quiz> quizzes = quizRepository.findByCategoryId(categoryId);
        return quizzes.stream()
                .filter(quiz -> quiz.getStatus() == QuizStatus.PUBLISHED)
                .collect(java.util.stream.Collectors.toMap(
                        Quiz::getId,
                        quiz -> quiz,
                        (existing, replacement) -> existing,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<QuizResponse> findAll() {
        return quizRepository.findAll().stream()
                .filter(quiz -> quiz.getStatus() == QuizStatus.PUBLISHED)
                .collect(java.util.stream.Collectors.toMap(
                        Quiz::getId,
                        quiz -> quiz,
                        (existing, replacement) -> existing,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<QuizResponse> findAllForAdmin() {
        return quizRepository.findAll().stream()
                .filter(quiz -> quiz.getStatus() != QuizStatus.ARCHIVED)
                .collect(java.util.stream.Collectors.toMap(
                        Quiz::getId,
                        quiz -> quiz,
                        (existing, replacement) -> existing,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<QuizResponse> findByCategoryForAdmin(UUID categoryId) {
        List<Quiz> quizzes = quizRepository.findByCategoryId(categoryId);
        return quizzes.stream()
                .filter(quiz -> quiz.getStatus() != QuizStatus.ARCHIVED)
                .collect(java.util.stream.Collectors.toMap(
                        Quiz::getId,
                        quiz -> quiz,
                        (existing, replacement) -> existing,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public QuizResponse get(UUID id) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Quiz not found");
        }
        return toResponse(quiz);
    }

    public QuizResponse getForAdmin(UUID id) {
        return toResponse(quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found")));
    }

    private void createVersion(Quiz quiz, QuizRequest request, Integer versionNo) {
        QuizVersion version = new QuizVersion();
        version.setQuiz(quiz);
        version.setVersionNo(versionNo);
        version.setTitleSnapshot(request.title());
        version.setDescriptionSnapshot(request.description());
        version.setDurationSeconds(request.durationSeconds());
        version.setPassingScore(request.passingScore());
        version.setStatus(QuizStatus.DRAFT);
        version = quizVersionRepository.save(version);

        quizVersionQuestionRepository.deleteByQuizVersionId(version.getId());
        List<UUID> questionIds = request.questionIds() == null || request.questionIds().isEmpty()
                ? getQuizCategories(quiz).stream()
                        .flatMap(category -> questionRepository.findByCategoryIdAndStatus(category.getId(), QuestionStatus.ACTIVE).stream())
                        .map(Question::getId)
                        .distinct()
                        .toList()
                : request.questionIds();
        if (questionIds.isEmpty()) {
            throw new BadRequestException("Add active questions to this category before creating or updating the quiz");
        }
        int displayOrder = 1;
        for (UUID questionId : questionIds) {
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
            QuizVersionQuestion link = new QuizVersionQuestion();
            link.setQuizVersion(version);
            link.setQuestion(question);
            link.setDisplayOrder(displayOrder++);
            link.setMarks(1);
            link.setNegativeMarks(0);
            link.setRequiredFlag(Boolean.TRUE);
            quizVersionQuestionRepository.save(link);
        }
        version.setQuestionCount(questionIds.size());
        quiz.setCurrentVersion(version);
        quizRepository.save(quiz);
        quizVersionRepository.save(version);
    }

    private QuizResponse toResponse(Quiz quiz) {
        List<Category> categories = getQuizCategories(quiz);
        int questionCount = 0;
        int easyCount = 0;
        int mediumCount = 0;
        int hardCount = 0;
        if (quiz.getCurrentVersion() != null) {
            List<QuizVersionQuestion> links = quizVersionQuestionRepository.findByQuizVersionIdOrderByDisplayOrderAsc(quiz.getCurrentVersion().getId());
            questionCount = links.size();
            for (QuizVersionQuestion link : links) {
                DifficultyLevel difficultyLevel = link.getQuestion().getDifficultyLevel();
                if (difficultyLevel == DifficultyLevel.EASY) {
                    easyCount++;
                } else if (difficultyLevel == DifficultyLevel.MEDIUM) {
                    mediumCount++;
                } else if (difficultyLevel == DifficultyLevel.HARD) {
                    hardCount++;
                }
            }
        }
        return new QuizResponse(
                quiz.getId(),
                quiz.getCategory().getId(),
                quiz.getCategory().getName(),
                categories.stream().map(Category::getId).toList(),
                categories.stream().map(Category::getName).toList(),
                quiz.getTitle(),
                quiz.getSlug(),
                quiz.getDescription(),
                quiz.getStatus().name(),
                quiz.getDurationSeconds(),
                quiz.getMaxAttemptsPerUser(),
                quiz.getAvailableFrom(),
                quiz.getAvailableTo(),
                Boolean.TRUE.equals(quiz.getPracticeMode()),
                Boolean.TRUE.equals(quiz.getCompetitionMode()),
                Boolean.TRUE.equals(quiz.getRequireFullScreen()),
                Boolean.TRUE.equals(quiz.getPreventTabSwitch()),
                Boolean.TRUE.equals(quiz.getRequireCamera()),
                Boolean.TRUE.equals(quiz.getRequireMicrophone()),
                Boolean.TRUE.equals(quiz.getRequireLocation()),
                Boolean.TRUE.equals(quiz.getCertificateEnabled()),
                Boolean.TRUE.equals(quiz.getCertificateAutoGenerate()),
                quiz.getCertificateDelayHours() == null ? 0 : quiz.getCertificateDelayHours(),
                quiz.getCertificateTemplate() == null ? null : quiz.getCertificateTemplate().getId(),
                quiz.getCertificateTemplate() == null ? null : quiz.getCertificateTemplate().getName(),
                quiz.getCurrentVersion() == null ? null : quiz.getCurrentVersion().getId(),
                questionCount,
                easyCount,
                mediumCount,
                hardCount,
                isCurrentlyAvailable(quiz)
        );
    }

    private List<Category> resolveCategories(QuizRequest request) {
        Set<UUID> categoryIds = new LinkedHashSet<>();
        if (request.categoryId() != null) {
            categoryIds.add(request.categoryId());
        }
        if (request.categoryIds() != null) {
            categoryIds.addAll(request.categoryIds());
        }
        if (categoryIds.isEmpty()) {
            throw new BadRequestException("Select at least one category");
        }
        List<Category> categories = categoryIds.stream()
                .map(categoryId -> categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId)))
                .toList();
        if (categories.isEmpty()) {
            throw new BadRequestException("Select at least one category");
        }
        return categories;
    }

    private void applyCategories(Quiz quiz, List<Category> categories) {
        quiz.setCategory(categories.get(0));
        quiz.getCategories().clear();
        quiz.getCategories().addAll(categories);
    }

    private List<Category> getQuizCategories(Quiz quiz) {
        if (quiz.getCategories() == null || quiz.getCategories().isEmpty()) {
            return List.of(quiz.getCategory());
        }
        return quiz.getCategories().stream().toList();
    }

    private void applyQuizMode(Quiz quiz, QuizRequest request) {
        boolean practiceMode = Boolean.TRUE.equals(request.practiceMode());
        boolean competitionMode = Boolean.TRUE.equals(request.competitionMode());
        if (practiceMode && competitionMode) {
            throw new BadRequestException("Quiz can be either Practice or Competition, not both");
        }
        quiz.setPracticeMode(practiceMode);
        applyCompetitionSettings(quiz, request, competitionMode);
    }

    private void applyCompetitionSettings(Quiz quiz, QuizRequest request, boolean competitionMode) {
        quiz.setCompetitionMode(competitionMode);
        quiz.setRequireFullScreen(competitionMode && Boolean.TRUE.equals(request.requireFullScreen()));
        quiz.setPreventTabSwitch(competitionMode && Boolean.TRUE.equals(request.preventTabSwitch()));
        quiz.setRequireCamera(competitionMode && Boolean.TRUE.equals(request.requireCamera()));
        quiz.setRequireMicrophone(competitionMode && Boolean.TRUE.equals(request.requireMicrophone()));
        quiz.setRequireLocation(competitionMode && Boolean.TRUE.equals(request.requireLocation()));
    }

    private void applyCertificateSettings(Quiz quiz, QuizRequest request) {
        if (Boolean.TRUE.equals(quiz.getPracticeMode())) {
            quiz.setCertificateEnabled(Boolean.FALSE);
            quiz.setCertificateAutoGenerate(Boolean.FALSE);
            quiz.setCertificateDelayHours(0);
            quiz.setCertificateTemplate(null);
            return;
        }
        boolean certificateEnabled = Boolean.TRUE.equals(request.certificateEnabled());
        quiz.setCertificateEnabled(certificateEnabled);
        quiz.setCertificateAutoGenerate(certificateEnabled && (request.certificateAutoGenerate() == null || Boolean.TRUE.equals(request.certificateAutoGenerate())));
        Integer delayHours = request.certificateDelayHours() == null ? 0 : request.certificateDelayHours();
        if (delayHours < 0 || delayHours > 168) {
            throw new BadRequestException("Certificate generation delay must be between 0 and 168 hours");
        }
        quiz.setCertificateDelayHours(certificateEnabled ? delayHours : 0);
        if (!certificateEnabled) {
            quiz.setCertificateTemplate(null);
            return;
        }
        if (request.certificateTemplateId() == null) {
            throw new BadRequestException("Certificate template is required when certificate is enabled");
        }
        CertificateTemplate template = certificateTemplateRepository.findById(request.certificateTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Certificate template not found"));
        if (!Boolean.TRUE.equals(template.getActive())) {
            throw new BadRequestException("Certificate template is inactive");
        }
        quiz.setCertificateTemplate(template);
    }

    private void validateAvailabilityWindow(Instant availableFrom, Instant availableTo) {
        Instant now = Instant.now();
        if (availableFrom != null && !availableFrom.isAfter(now)) {
            throw new BadRequestException("Available from must be a future date and time");
        }
        if (availableTo != null && !availableTo.isAfter(now)) {
            throw new BadRequestException("Available to must be a future date and time");
        }
        if (availableFrom != null && availableTo != null && !availableTo.isAfter(availableFrom)) {
            throw new BadRequestException("Available to must be after available from");
        }
    }

    private void ensureQuizCurrentlyAvailable(Quiz quiz) {
        Instant now = Instant.now();
        if (quiz.getAvailableFrom() != null && now.isBefore(quiz.getAvailableFrom())) {
            throw new BadRequestException("Quiz is not available yet");
        }
        if (quiz.getAvailableTo() != null && now.isAfter(quiz.getAvailableTo())) {
            throw new BadRequestException("Quiz availability has ended");
        }
    }

    private boolean isCurrentlyAvailable(Quiz quiz) {
        Instant now = Instant.now();
        if (quiz.getStatus() != QuizStatus.PUBLISHED || quiz.getCurrentVersion() == null) {
            return false;
        }
        if (quiz.getAvailableFrom() != null && now.isBefore(quiz.getAvailableFrom())) {
            return false;
        }
        if (quiz.getAvailableTo() != null && now.isAfter(quiz.getAvailableTo())) {
            return false;
        }
        return true;
    }

    private String uniqueSlug(String title) {
        return uniqueSlug(title, null);
    }

    private String uniqueSlug(String title, UUID currentQuizId) {
        String baseSlug = SlugUtil.toSlug(title);
        String slug = baseSlug;
        int suffix = 1;
        while (currentQuizId == null ? quizRepository.existsBySlug(slug) : quizRepository.existsBySlugAndIdNot(slug, currentQuizId)) {
            slug = baseSlug + "-" + suffix++;
        }
        return slug;
    }
}
