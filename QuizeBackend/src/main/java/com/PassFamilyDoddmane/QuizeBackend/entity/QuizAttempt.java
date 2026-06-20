package com.PassFamilyDoddmane.QuizeBackend.entity;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.AttemptStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.AutoSubmitReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_version_id", nullable = false)
    private QuizVersion quizVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "auto_submitted", nullable = false)
    private Boolean autoSubmitted = Boolean.FALSE;

    @Column(nullable = false)
    private Integer score = 0;

    @Column(name = "max_score", nullable = false)
    private Integer maxScore = 0;

    @Column(name = "correct_count", nullable = false)
    private Integer correctCount = 0;

    @Column(name = "wrong_count", nullable = false)
    private Integer wrongCount = 0;

    @Column(name = "unanswered_count", nullable = false)
    private Integer unansweredCount = 0;

    @Column(name = "elapsed_seconds")
    private Integer elapsedSeconds;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "auto_submit_reason", length = 64)
    private AutoSubmitReason autoSubmitReason;

    @Column(name = "proctoring_enabled", nullable = false)
    private Boolean proctoringEnabled = Boolean.FALSE;

    @OneToMany(mappedBy = "attempt", fetch = FetchType.LAZY)
    private List<AttemptAnswer> answers = new ArrayList<>();

    @OneToOne(mappedBy = "quizAttempt", fetch = FetchType.LAZY)
    private ProctoringSession proctoringSession;
}
