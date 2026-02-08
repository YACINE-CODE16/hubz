package com.hubz.infrastructure.persistence.entity;

import com.hubz.domain.enums.JobStatus;
import com.hubz.domain.enums.JobType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "background_jobs", indexes = {
    @Index(name = "idx_background_jobs_status", columnList = "status"),
    @Index(name = "idx_background_jobs_type", columnList = "type"),
    @Index(name = "idx_background_jobs_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackgroundJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(columnDefinition = "TEXT")
    private String error;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime executedAt;
}
