package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.EmailVerificationTokenRepositoryPort;
import com.hubz.domain.model.EmailVerificationToken;
import com.hubz.infrastructure.persistence.mapper.EmailVerificationTokenMapper;
import com.hubz.infrastructure.persistence.repository.EmailVerificationTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmailVerificationTokenRepositoryAdapter implements EmailVerificationTokenRepositoryPort {

    private final EmailVerificationTokenJpaRepository jpaRepository;
    private final EmailVerificationTokenMapper mapper;

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        var entity = mapper.toEntity(token);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<EmailVerificationToken> findByToken(String token) {
        return jpaRepository.findByToken(token)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<EmailVerificationToken> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteExpiredTokens() {
        jpaRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
