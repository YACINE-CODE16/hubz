package com.hubz.application.port.out;

import com.hubz.domain.model.PasswordResetToken;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepositoryPort {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    void deleteExpiredTokens();
}
