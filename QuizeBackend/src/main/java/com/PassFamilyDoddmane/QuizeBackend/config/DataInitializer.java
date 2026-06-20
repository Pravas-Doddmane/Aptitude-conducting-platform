package com.PassFamilyDoddmane.QuizeBackend.config;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.RoleName;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.QuestionStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.enums.UserStatus;
import com.PassFamilyDoddmane.QuizeBackend.entity.CertificateTemplate;
import com.PassFamilyDoddmane.QuizeBackend.entity.Question;
import com.PassFamilyDoddmane.QuizeBackend.entity.Quiz;
import com.PassFamilyDoddmane.QuizeBackend.entity.QuizVersionQuestion;
import com.PassFamilyDoddmane.QuizeBackend.entity.Role;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import com.PassFamilyDoddmane.QuizeBackend.entity.UserRole;
import com.PassFamilyDoddmane.QuizeBackend.repository.CertificateTemplateRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuestionRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizVersionQuestionRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizVersionRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.RoleRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.UserRepository;
import com.PassFamilyDoddmane.QuizeBackend.repository.UserRoleRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final CertificateTemplateRepository certificateTemplateRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizVersionQuestionRepository quizVersionQuestionRepository;
    private final QuizVersionRepository quizVersionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String adminPassword;

    @PostConstruct
    public void init() {
        ensureRole(RoleName.ADMIN, "Administrator");
        ensureRole(RoleName.USER, "User");

        String normalizedEmail = adminEmail == null || adminEmail.isBlank()
                ? "admin@gmail.com"
                : adminEmail.trim().toLowerCase();
        String bootstrapPassword = adminPassword == null || adminPassword.isBlank()
                ? "Admin@1234"
                : adminPassword;

        User admin = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(User::new);
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        admin.setStatus(UserStatus.ACTIVE);
        admin = userRepository.save(admin);

        Role adminRole = roleRepository.findByCode(RoleName.ADMIN).orElseThrow();
        boolean hasAdminRole = userRoleRepository.existsByUserIdAndRoleId(admin.getId(), adminRole.getId());
        if (!hasAdminRole) {
            UserRole membership = new UserRole();
            membership.setUser(admin);
            membership.setRole(adminRole);
            userRoleRepository.save(membership);
        }

        ensureDefaultCertificateTemplate();
    }

    private void ensureRole(RoleName code, String name) {
        roleRepository.findByCode(code).orElseGet(() -> {
            Role role = new Role();
            role.setCode(code);
            role.setName(name);
            return roleRepository.save(role);
        });
    }

    private void ensureDefaultCertificateTemplate() {
        if (certificateTemplateRepository.existsByNameIgnoreCase("Simple Achievement Certificate")) {
            return;
        }
        CertificateTemplate template = new CertificateTemplate();
        template.setName("Simple Achievement Certificate");
        template.setDescription("Default certificate for passed quiz participants");
        template.setHtmlTemplate("""
                <div class="certificate">
                  <img class="certificate-logo" src="{{logoUrl}}" alt="Certificate logo" />
                  <div class="subtitle">Certificate of Achievement</div>
                  <h1>{{fullName}}</h1>
                  <p>has successfully passed</p>
                  <h2>{{quizTitle}}</h2>
                  <p class="meta">Category: {{categoryName}}</p>
                  <p class="score">Score: {{score}} / {{maxScore}} ({{scorePercent}}%)</p>
                  <p class="date">Issued on {{issuedAt}}</p>
                </div>
                """);
        template.setCssTemplate("""
                .certificate {
                  width: 100%;
                  height: 100vh;
                  box-sizing: border-box;
                  padding: 70px;
                  text-align: center;
                  border: 18px solid #4f46e5;
                  position: relative;
                  color: #111827;
                  background: linear-gradient(135deg, #ffffff, #eef2ff);
                }
                .certificate-logo { position: absolute; top: 40px; right: 50px; width: 120px; max-height: 90px; object-fit: contain; }
                .certificate-logo[src=""] { display: none; }
                .subtitle { font-size: 34px; letter-spacing: 4px; text-transform: uppercase; color: #4f46e5; }
                h1 { font-size: 72px; margin: 70px 0 30px; font-family: Georgia, serif; }
                h2 { font-size: 46px; margin: 24px 0; }
                p { font-size: 28px; margin: 14px 0; }
                .score { font-weight: bold; color: #047857; }
                .date { margin-top: 70px; font-size: 24px; color: #6b7280; }
                """);
        template.setActive(Boolean.TRUE);
        certificateTemplateRepository.save(template);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void normalizeQuizDefaults() {
        for (Quiz quiz : quizRepository.findAll()) {
            boolean changed = false;
            if (quiz.getMaxAttemptsPerUser() == null) {
                quiz.setMaxAttemptsPerUser(0);
                changed = true;
            }
            if (quiz.getCompetitionMode() == null) {
                quiz.setCompetitionMode(Boolean.FALSE);
                changed = true;
            }
            if (quiz.getPracticeMode() == null) {
                quiz.setPracticeMode(Boolean.FALSE);
                changed = true;
            }
            if (Boolean.TRUE.equals(quiz.getPracticeMode()) && Boolean.TRUE.equals(quiz.getCompetitionMode())) {
                quiz.setCompetitionMode(Boolean.FALSE);
                changed = true;
            }
            if (quiz.getRequireFullScreen() == null) {
                quiz.setRequireFullScreen(Boolean.FALSE);
                changed = true;
            }
            if (quiz.getPreventTabSwitch() == null) {
                quiz.setPreventTabSwitch(Boolean.FALSE);
                changed = true;
            }
            if (quiz.getRequireCamera() == null) {
                quiz.setRequireCamera(Boolean.FALSE);
                changed = true;
            }
            if (quiz.getRequireMicrophone() == null) {
                quiz.setRequireMicrophone(Boolean.FALSE);
                changed = true;
            }
            if (quiz.getRequireLocation() == null) {
                quiz.setRequireLocation(Boolean.FALSE);
                changed = true;
            }
            if (!Boolean.TRUE.equals(quiz.getCompetitionMode())) {
                if (Boolean.TRUE.equals(quiz.getRequireFullScreen())) {
                    quiz.setRequireFullScreen(Boolean.FALSE);
                    changed = true;
                }
                if (Boolean.TRUE.equals(quiz.getPreventTabSwitch())) {
                    quiz.setPreventTabSwitch(Boolean.FALSE);
                    changed = true;
                }
                if (Boolean.TRUE.equals(quiz.getRequireCamera())) {
                    quiz.setRequireCamera(Boolean.FALSE);
                    changed = true;
                }
                if (Boolean.TRUE.equals(quiz.getRequireMicrophone())) {
                    quiz.setRequireMicrophone(Boolean.FALSE);
                    changed = true;
                }
                if (Boolean.TRUE.equals(quiz.getRequireLocation())) {
                    quiz.setRequireLocation(Boolean.FALSE);
                    changed = true;
                }
            }
            if (quiz.getCertificateEnabled() == null) {
                quiz.setCertificateEnabled(Boolean.FALSE);
                changed = true;
            }
            if (quiz.getCertificateAutoGenerate() == null) {
                quiz.setCertificateAutoGenerate(Boolean.TRUE);
                changed = true;
            }
            if (quiz.getCertificateDelayHours() == null) {
                quiz.setCertificateDelayHours(0);
                changed = true;
            }
            if (Boolean.TRUE.equals(quiz.getPracticeMode())) {
                if (Boolean.TRUE.equals(quiz.getCertificateEnabled())) {
                    quiz.setCertificateEnabled(Boolean.FALSE);
                    changed = true;
                }
                if (Boolean.TRUE.equals(quiz.getCertificateAutoGenerate())) {
                    quiz.setCertificateAutoGenerate(Boolean.FALSE);
                    changed = true;
                }
                if (quiz.getCertificateDelayHours() != null && quiz.getCertificateDelayHours() != 0) {
                    quiz.setCertificateDelayHours(0);
                    changed = true;
                }
                if (quiz.getCertificateTemplate() != null) {
                    quiz.setCertificateTemplate(null);
                    changed = true;
                }
            }
            if (quiz.getCategory() != null && (quiz.getCategories() == null || quiz.getCategories().isEmpty())) {
                quiz.getCategories().add(quiz.getCategory());
                changed = true;
            }
            if (changed) {
                quizRepository.save(quiz);
            }
            repairEmptyCurrentVersion(quiz);
        }
    }

    private void repairEmptyCurrentVersion(Quiz quiz) {
        if (quiz.getCurrentVersion() == null || quiz.getCategory() == null) {
            return;
        }
        if (!quizVersionQuestionRepository.findByQuizVersionIdOrderByDisplayOrderAsc(quiz.getCurrentVersion().getId()).isEmpty()) {
            return;
        }
        List<Question> questions = questionRepository.findByCategoryIdAndStatus(quiz.getCategory().getId(), QuestionStatus.ACTIVE);
        int displayOrder = 1;
        for (Question question : questions) {
            QuizVersionQuestion link = new QuizVersionQuestion();
            link.setQuizVersion(quiz.getCurrentVersion());
            link.setQuestion(question);
            link.setDisplayOrder(displayOrder++);
            link.setMarks(1);
            link.setNegativeMarks(0);
            link.setRequiredFlag(Boolean.TRUE);
            quizVersionQuestionRepository.save(link);
        }
        if (!questions.isEmpty()) {
            quiz.getCurrentVersion().setQuestionCount(questions.size());
            quizVersionRepository.save(quiz.getCurrentVersion());
        }
    }
}
