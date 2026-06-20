package com.PassFamilyDoddmane.QuizeBackend.entity;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
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
@Table(name = "identity_verifications", indexes = {
        @Index(name = "idx_identity_verification_session", columnList = "proctoring_session_id"),
        @Index(name = "idx_identity_verification_user", columnList = "user_id"),
        @Index(name = "idx_identity_verification_status", columnList = "verification_status")
})
public class IdentityVerification extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proctoring_session_id", nullable = false, unique = true)
    private ProctoringSession proctoringSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reference_image_url", nullable = false, length = 2000)
    private String referenceImageUrl;

    @Column(name = "reference_captured_at", nullable = false)
    private Instant referenceCapturedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 32)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "total_verifications", nullable = false)
    private Integer totalVerifications = 0;

    @Column(name = "successful_verifications", nullable = false)
    private Integer successfulVerifications = 0;

    @Column(name = "failed_verifications", nullable = false)
    private Integer failedVerifications = 0;

    @Column(name = "average_match_score")
    private Double averageMatchScore;

    @Column(name = "lowest_match_score")
    private Double lowestMatchScore;

    @Column(name = "last_verification_at")
    private Instant lastVerificationAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "identityVerification", fetch = FetchType.LAZY)
    private List<VerificationAttempt> verificationAttempts = new ArrayList<>();
}
