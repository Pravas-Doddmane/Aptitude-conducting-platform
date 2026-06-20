package com.PassFamilyDoddmane.QuizeBackend.entity;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.QuizStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "quizzes")
public class Quiz extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "quiz_categories",
            joinColumns = @JoinColumn(name = "quiz_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new LinkedHashSet<>();

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QuizStatus status = QuizStatus.DRAFT;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds = 600;

    @Column(name = "max_attempts_per_user")
    private Integer maxAttemptsPerUser = 0;

    @Column(name = "available_from")
    private Instant availableFrom;

    @Column(name = "available_to")
    private Instant availableTo;

    @Column(name = "practice_mode")
    private Boolean practiceMode = Boolean.FALSE;

    @Column(name = "competition_mode")
    private Boolean competitionMode = Boolean.FALSE;

    @Column(name = "require_full_screen")
    private Boolean requireFullScreen = Boolean.FALSE;

    @Column(name = "prevent_tab_switch")
    private Boolean preventTabSwitch = Boolean.FALSE;

    @Column(name = "require_camera")
    private Boolean requireCamera = Boolean.FALSE;

    @Column(name = "require_microphone")
    private Boolean requireMicrophone = Boolean.FALSE;

    @Column(name = "require_location")
    private Boolean requireLocation = Boolean.FALSE;

    @Column(name = "proctoring_enabled", nullable = false)
    private Boolean proctoringEnabled = Boolean.FALSE;

    @Column(name = "initial_lives")
    private Integer initialLives = 3;

    @Column(name = "certificate_enabled")
    private Boolean certificateEnabled = Boolean.FALSE;

    @Column(name = "certificate_auto_generate")
    private Boolean certificateAutoGenerate = Boolean.TRUE;

    @Column(name = "certificate_delay_hours")
    private Integer certificateDelayHours = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_template_id")
    private CertificateTemplate certificateTemplate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private QuizVersion currentVersion;

    @PrePersist
    @PreUpdate
    private void applyDefaults() {
        if (maxAttemptsPerUser == null) {
            maxAttemptsPerUser = 0;
        }
        if (competitionMode == null) {
            competitionMode = Boolean.FALSE;
        }
        if (practiceMode == null) {
            practiceMode = Boolean.FALSE;
        }
        if (requireFullScreen == null) {
            requireFullScreen = Boolean.FALSE;
        }
        if (preventTabSwitch == null) {
            preventTabSwitch = Boolean.FALSE;
        }
        if (requireCamera == null) {
            requireCamera = Boolean.FALSE;
        }
        if (requireMicrophone == null) {
            requireMicrophone = Boolean.FALSE;
        }
        if (requireLocation == null) {
            requireLocation = Boolean.FALSE;
        }
        if (proctoringEnabled == null) {
            proctoringEnabled = Boolean.FALSE;
        }
        if (initialLives == null) {
            initialLives = 3;
        }
        if (certificateEnabled == null) {
            certificateEnabled = Boolean.FALSE;
        }
        if (certificateAutoGenerate == null) {
            certificateAutoGenerate = Boolean.TRUE;
        }
        if (certificateDelayHours == null) {
            certificateDelayHours = 0;
        }
        if (Boolean.TRUE.equals(practiceMode) && Boolean.TRUE.equals(competitionMode)) {
            competitionMode = Boolean.FALSE;
        }
        if (Boolean.TRUE.equals(practiceMode)) {
            certificateEnabled = Boolean.FALSE;
            certificateAutoGenerate = Boolean.FALSE;
            certificateDelayHours = 0;
            certificateTemplate = null;
        }
        if (!Boolean.TRUE.equals(competitionMode)) {
            requireFullScreen = Boolean.FALSE;
            preventTabSwitch = Boolean.FALSE;
            requireCamera = Boolean.FALSE;
            requireMicrophone = Boolean.FALSE;
            requireLocation = Boolean.FALSE;
            proctoringEnabled = Boolean.FALSE;
        }
    }
}
