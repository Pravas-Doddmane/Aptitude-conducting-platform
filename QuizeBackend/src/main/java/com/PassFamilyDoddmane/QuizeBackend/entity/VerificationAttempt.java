package com.PassFamilyDoddmane.QuizeBackend.entity;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.VerificationResult;
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
@Table(name = "verification_attempts", indexes = {
        @Index(name = "idx_verification_attempt_identity", columnList = "identity_verification_id"),
        @Index(name = "idx_verification_attempt_timestamp", columnList = "verification_timestamp"),
        @Index(name = "idx_verification_attempt_result", columnList = "verification_result")
})
public class VerificationAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "identity_verification_id", nullable = false)
    private IdentityVerification identityVerification;

    @Column(name = "captured_image_url", nullable = false, length = 2000)
    private String capturedImageUrl;

    @Column(name = "verification_timestamp", nullable = false)
    private Instant verificationTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_result", nullable = false, length = 64)
    private VerificationResult verificationResult;

    @Column(name = "match_score")
    private Double matchScore;

    @Column(name = "confidence_level")
    private Double confidenceLevel;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "evidence_url", length = 2000)
    private String evidenceUrl;

    @Column(name = "triggered_violation", nullable = false)
    private Boolean triggeredViolation = Boolean.FALSE;

    @Column(name = "violation_id")
    private java.util.UUID violationId;
}
