package com.PassFamilyDoddmane.QuizeBackend.entity;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.QuizStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "quiz_versions")
public class QuizVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "title_snapshot", nullable = false, length = 160)
    private String titleSnapshot;

    @Column(name = "description_snapshot", length = 2000)
    private String descriptionSnapshot;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "passing_score")
    private Integer passingScore;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QuizStatus status = QuizStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;
}
