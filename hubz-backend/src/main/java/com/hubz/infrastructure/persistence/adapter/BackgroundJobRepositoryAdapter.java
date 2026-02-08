package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.BackgroundJobRepositoryPort;
import com.hubz.domain.enums.JobStatus;
import com.hubz.domain.model.BackgroundJob;
import com.hubz.infrastructure.persistence.mapper.BackgroundJobMapper;
import com.hubz.infrastructure.persistence.repository.BackgroundJobJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BackgroundJobRepositoryAdapter implements BackgroundJobRepositoryPort {

    private final BackgroundJobJpaRepository jpaRepository;
    private final BackgroundJobMapper mapper;

    @Override
    public BackgroundJob save(BackgroundJob job) {
        var entity = mapper.toEntity(job);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<BackgroundJob> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<BackgroundJob> findByStatus(JobStatus status) {
        return jpaRepository.findByStatusOrderByCreatedAtAsc(status).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<BackgroundJob> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<BackgroundJob> findFailedJobsForRetry(int maxRetries) {
        return jpaRepository.findFailedJobsForRetry(maxRetries).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public int deleteByCreatedAtBefore(LocalDateTime cutoffDate) {
        return jpaRepository.deleteByCreatedAtBefore(cutoffDate);
    }

    @Override
    public long countByStatus(JobStatus status) {
        return jpaRepository.countByStatus(status);
    }
}
