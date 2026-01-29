package com.hubz.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;
    private String icon;
    private String color;

    @Column(columnDefinition = "TEXT")
    private String readme;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Note: Cascade delete is handled at database level via foreign key constraints
    // To use JPA cascade, would need to refactor to use @ManyToOne entity references
    // instead of UUID fields in OrganizationMemberEntity and TaskEntity
}
