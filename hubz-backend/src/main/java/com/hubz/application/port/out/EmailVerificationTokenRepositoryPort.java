package com.hubz.application.port.out;

import com.hubz.domain.model.EmailVerificationToken;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepositoryPort {

    EmailVerificationToken save(EmailVerificationToken token);

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    void deleteExpiredTokens();
}
