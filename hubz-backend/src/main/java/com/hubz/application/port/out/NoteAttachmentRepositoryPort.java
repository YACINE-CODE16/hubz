package com.hubz.application.port.out;

import com.hubz.domain.model.NoteAttachment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteAttachmentRepositoryPort {
    NoteAttachment save(NoteAttachment attachment);
    Optional<NoteAttachment> findById(UUID id);
    List<NoteAttachment> findByNoteId(UUID noteId);
    void deleteById(UUID id);
}
