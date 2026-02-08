package com.hubz.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "note_folders", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "organization_id", "parent_folder_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteFolderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "parent_folder_id")
    private UUID parentFolderId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "created_by_id", nullable = false)
    private UUID createdById;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
