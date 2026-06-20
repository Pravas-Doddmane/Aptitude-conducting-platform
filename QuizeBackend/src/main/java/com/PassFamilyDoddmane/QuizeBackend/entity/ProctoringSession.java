package com.PassFamilyDoddmane.QuizeBackend.entity;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.ProctoringSessionStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
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
@Table(name = "proctoring_sessions", indexes = {
        @Index(name = "idx_proctoring_session_attempt", columnList = "quiz_attempt_id"),
        @Index(name = "idx_proctoring_session_status", columnList = "status"),
        @Index(name = "idx_proctoring_session_risk", columnList = "risk_level")
})
public class ProctoringSession extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_attempt_id", nullable = false, unique = true)
    private QuizAttempt quizAttempt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProctoringSessionStatus status = ProctoringSessionStatus.ACTIVE;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "initial_lives", nullable = false)
    private Integer initialLives = 3;

    @Column(name = "remaining_lives", nullable = false)
    private Integer remainingLives = 3;

    @Column(name = "total_violations", nullable = false)
    private Integer totalViolations = 0;

    @Column(name = "critical_violations", nullable = false)
    private Integer criticalViolations = 0;

    @Column(name = "warning_violations", nullable = false)
    private Integer warningViolations = 0;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "proctoring_enabled", nullable = false)
    private Boolean proctoringEnabled = Boolean.TRUE;

    @Column(name = "session_token", length = 500)
    private String sessionToken;

    @Column(name = "python_service_url", length = 500)
    private String pythonServiceUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "proctoringSession", fetch = FetchType.LAZY)
    private List<ProctoringEvent> events = new ArrayList<>();

    @OneToMany(mappedBy = "proctoringSession", fetch = FetchType.LAZY)
    private List<ProctoringViolation> violations = new ArrayList<>();

    @OneToOne(mappedBy = "proctoringSession", fetch = FetchType.LAZY)
    private IdentityVerification identityVerification;
}
