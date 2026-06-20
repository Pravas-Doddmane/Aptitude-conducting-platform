package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.AttemptStatus;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.BadRequestException;
import com.PassFamilyDoddmane.QuizeBackend.common.exception.ResourceNotFoundException;
import com.PassFamilyDoddmane.QuizeBackend.entity.CertificateTemplate;
import com.PassFamilyDoddmane.QuizeBackend.entity.Quiz;
import com.PassFamilyDoddmane.QuizeBackend.entity.QuizAttempt;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import com.PassFamilyDoddmane.QuizeBackend.repository.QuizAttemptRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CertificateService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final CurrentUserService currentUserService;

    public CertificateFile download(UUID attemptId, String format, String customName) {
        User user = currentUserService.getCurrentUser();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));
        validateCertificateAllowed(attempt);

        String safeFormat = format == null || format.isBlank() ? "pdf" : format.toLowerCase(Locale.ROOT);
        Quiz quiz = attempt.getQuizVersion().getQuiz();
        String fileBaseName = slug(quiz.getTitle()) + "-certificate";
        String html = renderCertificateHtml(attempt, customName);
        if ("html".equals(safeFormat)) {
            return new CertificateFile(fileBaseName + ".html", "text/html", html.getBytes(StandardCharsets.UTF_8));
        }
        if ("png".equals(safeFormat) || "photo".equals(safeFormat) || "image".equals(safeFormat)) {
            return new CertificateFile(fileBaseName + ".png", "image/png", renderPng(attempt, customName));
        }
        return new CertificateFile(fileBaseName + ".pdf", "application/pdf", renderPdf(html));
    }

    private void validateCertificateAllowed(QuizAttempt attempt) {
        if (attempt.getStatus() != AttemptStatus.SUBMITTED && attempt.getStatus() != AttemptStatus.AUTO_SUBMITTED) {
            throw new BadRequestException("Certificate is available only after quiz submission");
        }
        Quiz quiz = attempt.getQuizVersion().getQuiz();
        if (!Boolean.TRUE.equals(quiz.getCertificateEnabled()) || quiz.getCertificateTemplate() == null) {
            throw new BadRequestException("Certificate is not enabled for this quiz");
        }
        int passingScore = attempt.getQuizVersion().getPassingScore() == null ? 0 : attempt.getQuizVersion().getPassingScore();
        double scorePercent = attempt.getMaxScore() == null || attempt.getMaxScore() == 0
                ? 0
                : (attempt.getScore() * 100.0) / attempt.getMaxScore();
        if (scorePercent < passingScore) {
            throw new BadRequestException("Certificate is available only for users who passed the quiz");
        }
        int delayHours = quiz.getCertificateDelayHours() == null ? 0 : quiz.getCertificateDelayHours();
        if (delayHours > 0 && attempt.getSubmittedAt() != null) {
            Instant availableAt = attempt.getSubmittedAt().plusSeconds(delayHours * 3600L);
            if (Instant.now().isBefore(availableAt)) {
                throw new BadRequestException("Certificate will be available after " + delayHours + " hour(s) from quiz completion");
            }
        }
    }

    private String renderCertificateHtml(QuizAttempt attempt, String customName) {
        CertificateTemplate template = attempt.getQuizVersion().getQuiz().getCertificateTemplate();
        String body = replaceVariables(template.getHtmlTemplate(), variables(attempt, customName));
        String css = replaceVariables(template.getCssTemplate() == null ? "" : template.getCssTemplate(), variables(attempt, customName));
        return """
                <!doctype html>
                <html>
                  <head>
                    <meta charset="UTF-8" />
                    <style>
                      @page { size: A4 landscape; margin: 0; }
                      body { margin: 0; font-family: Arial, sans-serif; }
                      %s
                    </style>
                  </head>
                  <body>%s</body>
                </html>
                """.formatted(css, body);
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Unable to generate certificate PDF: " + ex.getMessage());
        }
    }

    private byte[] renderPng(QuizAttempt attempt, String customName) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Map<String, String> values = variables(attempt, customName);
            BufferedImage image = new BufferedImage(1600, 1100, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(248, 250, 252));
            graphics.fillRect(0, 0, 1600, 1100);
            graphics.setColor(new Color(79, 70, 229));
            graphics.fillRect(70, 70, 1460, 960);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(105, 105, 1390, 890);
            graphics.setColor(new Color(15, 23, 42));
            graphics.setFont(new Font("Serif", Font.BOLD, 72));
            drawCentered(graphics, "Certificate of Achievement", 800, 240);
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 34));
            drawCentered(graphics, "This certificate is proudly presented to", 800, 345);
            graphics.setFont(new Font("Serif", Font.BOLD, 76));
            drawCentered(graphics, values.get("fullName"), 800, 465);
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 34));
            drawCentered(graphics, "for successfully passing", 800, 560);
            graphics.setFont(new Font("SansSerif", Font.BOLD, 48));
            drawCentered(graphics, values.get("quizTitle"), 800, 635);
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 30));
            drawCentered(graphics, "Score: " + values.get("score") + "/" + values.get("maxScore") + " (" + values.get("scorePercent") + "%)", 800, 735);
            drawCentered(graphics, "Issued on " + values.get("issuedAt"), 800, 820);
            graphics.dispose();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Unable to generate certificate image: " + ex.getMessage());
        }
    }

    private void drawCentered(Graphics2D graphics, String text, int centerX, int y) {
        int width = graphics.getFontMetrics().stringWidth(text == null ? "" : text);
        graphics.drawString(text == null ? "" : text, centerX - width / 2, y);
    }

    private String replaceVariables(String template, Map<String, String> variables) {
        String rendered = template == null ? "" : template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", escapeHtml(entry.getValue()));
        }
        return rendered;
    }

    private Map<String, String> variables(QuizAttempt attempt, String customName) {
        User user = attempt.getUser();
        CertificateTemplate template = attempt.getQuizVersion().getQuiz().getCertificateTemplate();
        String firstName = user.getProfile() == null || user.getProfile().getFirstName() == null ? "" : user.getProfile().getFirstName();
        String lastName = user.getProfile() == null || user.getProfile().getLastName() == null ? "" : user.getProfile().getLastName();
        String fullName = (firstName + " " + lastName).trim();
        
        // Use custom name if provided, otherwise fallback to profile name or email
        if (customName != null && !customName.isBlank()) {
            fullName = customName.trim();
        } else if (fullName.isBlank()) {
            fullName = user.getEmail();
        }
        
        double scorePercent = attempt.getMaxScore() == null || attempt.getMaxScore() == 0
                ? 0
                : (attempt.getScore() * 100.0) / attempt.getMaxScore();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("firstName", firstName);
        values.put("lastName", lastName);
        values.put("fullName", fullName);
        values.put("email", user.getEmail());
        values.put("logoUrl", template == null || template.getLogoUrl() == null ? "" : template.getLogoUrl());
        values.put("quizTitle", attempt.getQuizVersion().getQuiz().getTitle());
        values.put("categoryName", attempt.getQuizVersion().getQuiz().getCategory().getName());
        values.put("score", String.valueOf(attempt.getScore()));
        values.put("maxScore", String.valueOf(attempt.getMaxScore()));
        values.put("scorePercent", String.format(Locale.US, "%.2f", scorePercent));
        values.put("issuedAt", DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault()).format(attempt.getSubmittedAt()));
        return values;
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String slug(String value) {
        return value == null ? "quiz" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    public record CertificateFile(String fileName, String contentType, byte[] content) {
    }
}
