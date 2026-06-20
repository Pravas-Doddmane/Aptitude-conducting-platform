package com.PassFamilyDoddmane.QuizeBackend.entity;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.EventSeverity;
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
@Table(name = "proctoring_events", indexes = {
        @Index(name = "idx_proctoring_event_session", columnList = "proctoring_session_id"),
        @Index(name = "idx_proctoring_event_timestamp", columnList = "event_timestamp"),
        @Index(name = "idx_proctoring_event_type", columnList = "event_type")
})
public class ProctoringEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proctoring_session_id", nullable = false)
    private ProctoringSession proctoringSession;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventSeverity severity = EventSeverity.INFO;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "frame_data_url", length = 2000)
    private String frameDataUrl;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "lives_deducted", nullable = false)
    private Integer livesDeducted = 0;

    @Column(name = "remaining_lives_snapshot")
    private Integer remainingLivesSnapshot;
}
