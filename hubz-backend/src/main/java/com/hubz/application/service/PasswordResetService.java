package com.hubz.application.service;

import com.hubz.application.dto.request.ForgotPasswordRequest;
import com.hubz.application.dto.request.ResetPasswordRequest;
import com.hubz.application.dto.response.MessageResponse;
import com.hubz.application.port.out.PasswordResetTokenRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.InvalidTokenException;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.PasswordResetToken;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int TOKEN_EXPIRATION_HOURS = 1;

    private final PasswordResetTokenRepositoryPort tokenRepository;
    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public MessageResponse requestPasswordReset(ForgotPasswordRequest request) {
        // Always return success to prevent email enumeration attacks
        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElse(null);

            if (user != null) {
                // Delete any existing tokens for this user
                tokenRepository.deleteByUserId(user.getId());

                // Create new token
                PasswordResetToken token = PasswordResetToken.builder()
                        .id(UUID.randomUUID())
                        .userId(user.getId())
                        .token(UUID.randomUUID().toString())
                        .createdAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS))
                        .used(false)
                        .build();

                tokenRepository.save(token);

                // Send email
                emailService.sendPasswordResetEmail(
                        user.getEmail(),
                        user.getFirstName(),
                        token.getToken()
                );

                log.info("Password reset email sent to {}", request.getEmail());
            } else {
                log.info("Password reset requested for non-existent email: {}", request.getEmail());
            }
        } catch (Exception e) {
            log.error("Error during password reset request for {}", request.getEmail(), e);
            // Still return success to prevent enumeration
        }

        return MessageResponse.success(
                "Si un compte existe avec cette adresse email, vous recevrez un lien de réinitialisation."
        );
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(InvalidTokenException::new);

        if (!token.isValid()) {
            throw new InvalidTokenException();
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UserNotFoundException(token.getUserId()));

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Mark token as used
        token.setUsed(true);
        tokenRepository.save(token);

        log.info("Password reset successful for user {}", user.getEmail());

        return MessageResponse.success("Votre mot de passe a été réinitialisé avec succès.");
    }

    public boolean isTokenValid(String token) {
        return tokenRepository.findByToken(token)
                .map(PasswordResetToken::isValid)
                .orElse(false);
    }
}
