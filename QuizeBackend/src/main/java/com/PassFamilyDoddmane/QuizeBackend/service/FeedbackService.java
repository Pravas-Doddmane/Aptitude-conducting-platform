package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.AuditAction;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.FeedbackStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.BadRequestException;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.dto.feedback.FeedbackRequest;
import com.PassFamilyDoddmane.QuizeBackend.dto.feedback.FeedbackResponse;
import com.PassFamilyDoddmane.QuizeBackend.dto.feedback.FeedbackStatusUpdateRequest;
import com.PassFamilyDoddmane.QuizeBackend.entity.Feedback;
import com.PassFamilyDoddmane.QuizeBackend.entity.Question;
import com.PassFamilyDoddmane.QuizeBackend.entity.Quiz;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import com.PassFamilyDoddmane.QuizeBackend.repository.FeedbackRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuestionRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public FeedbackResponse submit(FeedbackRequest request) {
        if (request.quizId() == null && request.questionId() == null) {
            throw new BadRequestException("Quiz or question must be selected");
        }

        User user = currentUserService.getCurrentUser();
        Quiz quiz = null;
        Question question = null;

        if (request.quizId() != null) {
            quiz = quizRepository.findById(request.quizId())
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        }
        if (request.questionId() != null) {
            question = questionRepository.findById(request.questionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        }

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setQuiz(quiz);
        feedback.setQuestion(question);
        feedback.setRating(request.rating());
        feedback.setMessage(request.message());
        feedback.setStatus(FeedbackStatus.NEW);

        Feedback saved = feedbackRepository.save(feedback);
        FeedbackResponse response = toResponse(saved);
        auditService.logAction(AuditAction.CREATE, "Feedback", saved.getId().toString(), null, response);
        return response;
    }

    public List<FeedbackResponse> findAll(FeedbackStatus status) {
        List<Feedback> feedbacks = status == null
                ? feedbackRepository.findAllByOrderByCreatedAtDesc()
                : feedbackRepository.findByStatusOrderByCreatedAtDesc(status);
        return feedbacks.stream().map(this::toResponse).toList();
    }

    public FeedbackResponse updateStatus(UUID id, FeedbackStatusUpdateRequest request) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found"));
        FeedbackStatus before = feedback.getStatus();
        feedback.setStatus(request.status());
        feedback.setAdminNote(request.adminNote());
        feedback.setReviewedAt(Instant.now());
        feedback.setReviewedBy(currentUserService.getCurrentUser());
        Feedback saved = feedbackRepository.save(feedback);
        FeedbackResponse response = toResponse(saved);
        auditService.logAction(AuditAction.UPDATE, "Feedback", saved.getId().toString(), before.name(), response);
        return response;
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        String userName = feedback.getUser().getProfile() == null
                ? feedback.getUser().getEmail()
                : buildName(feedback.getUser());
        String reviewedByName = feedback.getReviewedBy() == null
                ? null
                : (feedback.getReviewedBy().getProfile() == null ? feedback.getReviewedBy().getEmail() : buildName(feedback.getReviewedBy()));
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getUser().getId(),
                userName,
                feedback.getUser().getEmail(),
                feedback.getQuiz() == null ? null : feedback.getQuiz().getId(),
                feedback.getQuiz() == null ? null : feedback.getQuiz().getTitle(),
                feedback.getQuestion() == null ? null : feedback.getQuestion().getId(),
                feedback.getQuestion() == null ? null : feedback.getQuestion().getStem(),
                feedback.getRating(),
                feedback.getMessage(),
                feedback.getStatus().name(),
                feedback.getAdminNote(),
                feedback.getReviewedBy() == null ? null : feedback.getReviewedBy().getId(),
                reviewedByName,
                feedback.getReviewedAt(),
                feedback.getCreatedAt()
        );
    }

    private String buildName(User user) {
        String firstName = user.getProfile().getFirstName();
        String lastName = user.getProfile().getLastName();
        String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        return fullName.isBlank() ? user.getEmail() : fullName;
    }
}
