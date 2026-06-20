package com.PassFamilyDoddmane.QuizeBackend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetToken(String toEmail, String resetToken) {
        sendSimpleHtmlMail(
                toEmail,
                "Quiz Backend OTP for password reset",
                "<p>Your password reset OTP is:</p><p><b>" + resetToken + "</b></p>"
                        + "<p>This OTP expires in 1 hour.</p>"
        );
    }

    public void sendRegistrationOtp(String toEmail, String otp) {
        sendSimpleHtmlMail(
                toEmail,
                "Quiz account email verification OTP",
                "<p>Your email verification OTP is:</p><p><b>" + otp + "</b></p>"
                        + "<p>This OTP expires in 15 minutes.</p>"
        );
    }

    private void sendSimpleHtmlMail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Unable to send email", ex);
        }
    }
}
