package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.EmailVerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationTokenEntity, UUID> {

    Optional<EmailVerificationTokenEntity> findByToken(String token);

    Optional<EmailVerificationTokenEntity> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationTokenEntity t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);
}
