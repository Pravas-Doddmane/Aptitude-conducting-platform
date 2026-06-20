package com.PassFamilyDoddmane.QuizeBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quiz_version_questions")
public class QuizVersionQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_version_id", nullable = false)
    private QuizVersion quizVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private Integer marks = 1;

    @Column(name = "negative_marks", nullable = false)
    private Integer negativeMarks = 0;

    @Column(name = "required_flag", nullable = false)
    private Boolean requiredFlag = Boolean.TRUE;
}
