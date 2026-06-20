package com.PassFamilyDoddmane.QuizeBackend.entity;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.ViolationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "proctoring_violations", indexes = {
        @Index(name = "idx_proctoring_violation_session", columnList = "proctoring_session_id"),
        @Index(name = "idx_proctoring_violation_type", columnList = "violation_type"),
        @Index(name = "idx_proctoring_violation_timestamp", columnList = "violation_timestamp")
})
public class ProctoringViolation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proctoring_session_id", nullable = false)
    private ProctoringSession proctoringSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", nullable = false, length = 64)
    private ViolationType violationType;

    @Column(name = "violation_timestamp", nullable = false)
    private Instant violationTimestamp;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "snapshot_url", length = 2000)
    private String snapshotUrl;

    @Column(name = "evidence_url", length = 2000)
    private String evidenceUrl;

    @Column(name = "evidence_data", columnDefinition = "TEXT")
    private String evidenceData;

    @Column(name = "lives_deducted", nullable = false)
    private Integer livesDeducted = 1;

    @Column(name = "remaining_lives_after", nullable = false)
    private Integer remainingLivesAfter;

    @Column(name = "auto_flagged", nullable = false)
    private Boolean autoFlagged = Boolean.TRUE;

    @Column(name = "reviewed", nullable = false)
    private Boolean reviewed = Boolean.FALSE;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;
}
