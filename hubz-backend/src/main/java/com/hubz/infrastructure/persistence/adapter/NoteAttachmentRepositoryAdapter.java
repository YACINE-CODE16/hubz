package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.NoteAttachmentRepositoryPort;
import com.hubz.domain.model.NoteAttachment;
import com.hubz.infrastructure.persistence.mapper.NoteAttachmentMapper;
import com.hubz.infrastructure.persistence.repository.JpaNoteAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NoteAttachmentRepositoryAdapter implements NoteAttachmentRepositoryPort {

    private final JpaNoteAttachmentRepository jpaRepository;
    private final NoteAttachmentMapper mapper;

    @Override
    public NoteAttachment save(NoteAttachment attachment) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(attachment)));
    }

    @Override
    public Optional<NoteAttachment> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<NoteAttachment> findByNoteId(UUID noteId) {
        return jpaRepository.findByNoteId(noteId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
