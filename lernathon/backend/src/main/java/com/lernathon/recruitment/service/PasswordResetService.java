package com.lernathon.recruitment.service;

import com.lernathon.recruitment.entity.PasswordResetToken;
import com.lernathon.recruitment.entity.User;
import com.lernathon.recruitment.repository.PasswordResetTokenRepository;
import com.lernathon.recruitment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    private static final int TOKEN_EXPIRY_MINUTES = 15;

    @Transactional
    public void initiatePasswordReset(String email) {
        // Always return generic message – do not reveal whether email exists
        userRepository.findByEmail(email).ifPresent(user -> {
            // Invalidate all old tokens for this user
            tokenRepository.invalidateAllTokensForUser(user);

            // Create new token
            String rawToken = UUID.randomUUID().toString().replace("-", "");
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(rawToken)
                    .expiryDate(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES))
                    .used(false)
                    .build();
            tokenRepository.save(resetToken);

            // Send email
            String resetLink = baseUrl + "/reset-password?token=" + rawToken;
            try {
                emailService.sendPasswordResetEmail(email, resetLink);
                log.info("Password reset email sent to: {}", email);
            } catch (Exception e) {
                log.error("Failed to send password reset email to: {}", email, e);
            }
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("This reset link has already been used");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("This reset link has expired. Please request a new one");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getEmail());
    }
}
