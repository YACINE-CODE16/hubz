package com.hubz.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersionEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID documentId;

    @Column(nullable = false)
    private Integer versionNumber;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private Long fileSize;

    private String contentType;

    @Column(nullable = false)
    private UUID uploadedBy;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;
}
