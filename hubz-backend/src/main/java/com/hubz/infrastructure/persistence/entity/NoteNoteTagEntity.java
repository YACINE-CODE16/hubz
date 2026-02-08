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
@Table(name = "note_note_tags", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"note_id", "note_tag_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteNoteTagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "note_id", nullable = false)
    private UUID noteId;

    @Column(name = "note_tag_id", nullable = false)
    private UUID noteTagId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
