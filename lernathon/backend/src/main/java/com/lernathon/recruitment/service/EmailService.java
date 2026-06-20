package com.lernathon.recruitment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("HireAI - Password Reset Request");
        message.setText(
            "Hello,\n\n" +
            "We received a request to reset your password for your HireAI account.\n\n" +
            "Click the link below to reset your password (valid for 15 minutes):\n\n" +
            resetLink + "\n\n" +
            "If you did not request a password reset, please ignore this email. " +
            "Your password will remain unchanged.\n\n" +
            "This link will expire in 15 minutes and can only be used once.\n\n" +
            "Best regards,\n" +
            "The HireAI Team"
        );
        mailSender.send(message);
    }
}
