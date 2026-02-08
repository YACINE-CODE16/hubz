package com.hubz.infrastructure.persistence.repository;

import com.hubz.domain.enums.JobStatus;
import com.hubz.infrastructure.persistence.entity.BackgroundJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BackgroundJobJpaRepository extends JpaRepository<BackgroundJobEntity, UUID> {

    List<BackgroundJobEntity> findByStatusOrderByCreatedAtAsc(JobStatus status);

    @Query("SELECT j FROM BackgroundJobEntity j WHERE j.status = 'FAILED' AND j.retryCount < :maxRetries ORDER BY j.createdAt ASC")
    List<BackgroundJobEntity> findFailedJobsForRetry(@Param("maxRetries") int maxRetries);

    @Modifying
    @Query("DELETE FROM BackgroundJobEntity j WHERE j.createdAt < :cutoffDate")
    int deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    long countByStatus(JobStatus status);
}
