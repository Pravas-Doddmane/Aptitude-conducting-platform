package com.PassFamilyDoddmane.QuizeBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "attempt_answers")
public class AttemptAnswer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private QuestionOption selectedOption;

    @Column(name = "is_correct", nullable = false)
    private Boolean correct = Boolean.FALSE;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "question_snapshot_json", columnDefinition = "text")
    private String questionSnapshotJson;

    @Column(name = "option_snapshot_json", columnDefinition = "text")
    private String optionSnapshotJson;
}
