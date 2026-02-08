package com.hubz.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "direct_messages", indexes = {
        @Index(name = "idx_dm_sender_id", columnList = "sender_id"),
        @Index(name = "idx_dm_receiver_id", columnList = "receiver_id"),
        @Index(name = "idx_dm_created_at", columnList = "created_at"),
        @Index(name = "idx_dm_sender_receiver", columnList = "sender_id, receiver_id"),
        @Index(name = "idx_dm_receiver_read", columnList = "receiver_id, is_read")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;
}
