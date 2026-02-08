package com.hubz.application.service;

import com.hubz.application.dto.request.ResendVerificationRequest;
import com.hubz.application.dto.response.MessageResponse;
import com.hubz.application.port.out.EmailVerificationTokenRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.EmailAlreadyVerifiedException;
import com.hubz.domain.exception.InvalidTokenException;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.EmailVerificationToken;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private static final int TOKEN_EXPIRATION_HOURS = 24;

    private final EmailVerificationTokenRepositoryPort tokenRepository;
    private final UserRepositoryPort userRepository;
    private final EmailService emailService;

    @Transactional
    public void createAndSendVerificationToken(User user) {
        // Delete any existing tokens for this user
        tokenRepository.deleteByUserId(user.getId());

        // Create new token
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .token(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS))
                .used(false)
                .build();

        tokenRepository.save(token);

        // Send email
        try {
            emailService.sendEmailVerificationEmail(
                    user.getEmail(),
                    user.getFirstName(),
                    token.getToken()
            );
            log.info("Email verification sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send email verification to {}", user.getEmail(), e);
            // Don't throw - the user can resend the verification email later
        }
    }

    @Transactional
    public MessageResponse verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(InvalidTokenException::new);

        if (!verificationToken.isValid()) {
            throw new InvalidTokenException();
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(verificationToken.getUserId()));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailAlreadyVerifiedException();
        }

        // Update user email verification status
        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Mark token as used
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        log.info("Email verified successfully for user {}", user.getEmail());

        return MessageResponse.success("Votre adresse email a été vérifiée avec succès.");
    }

    @Transactional
    public MessageResponse resendVerificationEmail(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // Always return success to prevent email enumeration
        if (user == null) {
            log.info("Resend verification requested for non-existent email: {}", request.getEmail());
            return MessageResponse.success(
                    "Si un compte existe avec cette adresse email, vous recevrez un email de vérification."
            );
        }

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            // User's email is already verified - don't reveal this info
            log.info("Resend verification requested for already verified email: {}", request.getEmail());
            return MessageResponse.success(
                    "Si un compte existe avec cette adresse email, vous recevrez un email de vérification."
            );
        }

        createAndSendVerificationToken(user);

        return MessageResponse.success(
                "Si un compte existe avec cette adresse email, vous recevrez un email de vérification."
        );
    }

    public boolean isTokenValid(String token) {
        return tokenRepository.findByToken(token)
                .map(EmailVerificationToken::isValid)
                .orElse(false);
    }
}
