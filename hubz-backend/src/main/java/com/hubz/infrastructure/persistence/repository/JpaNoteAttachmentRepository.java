package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.NoteAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaNoteAttachmentRepository extends JpaRepository<NoteAttachmentEntity, UUID> {
    List<NoteAttachmentEntity> findByNoteId(UUID noteId);
}
