package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.NoteTagRepositoryPort;
import com.hubz.domain.model.NoteTag;
import com.hubz.infrastructure.persistence.entity.NoteNoteTagEntity;
import com.hubz.infrastructure.persistence.mapper.NoteTagMapper;
import com.hubz.infrastructure.persistence.repository.NoteNoteTagJpaRepository;
import com.hubz.infrastructure.persistence.repository.NoteTagJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NoteTagRepositoryAdapter implements NoteTagRepositoryPort {

    private final NoteTagJpaRepository noteTagJpaRepository;
    private final NoteNoteTagJpaRepository noteNoteTagJpaRepository;
    private final NoteTagMapper mapper;

    @Override
    public NoteTag save(NoteTag tag) {
        var entity = mapper.toEntity(tag);
        var saved = noteTagJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<NoteTag> findById(UUID id) {
        return noteTagJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<NoteTag> findByOrganizationId(UUID organizationId) {
        return noteTagJpaRepository.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<NoteTag> findByIds(Set<UUID> ids) {
        return noteTagJpaRepository.findByIdIn(ids).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        // First delete all note-tag associations
        noteNoteTagJpaRepository.deleteAllByNoteTagId(id);
        // Then delete the tag
        noteTagJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByNameAndOrganizationId(String name, UUID organizationId) {
        return noteTagJpaRepository.existsByNameAndOrganizationId(name, organizationId);
    }

    @Override
    public void addTagToNote(UUID noteId, UUID tagId) {
        if (!noteNoteTagJpaRepository.existsByNoteIdAndNoteTagId(noteId, tagId)) {
            NoteNoteTagEntity entity = NoteNoteTagEntity.builder()
                    .id(UUID.randomUUID())
                    .noteId(noteId)
                    .noteTagId(tagId)
                    .createdAt(LocalDateTime.now())
                    .build();
            noteNoteTagJpaRepository.save(entity);
        }
    }

    @Override
    public void removeTagFromNote(UUID noteId, UUID tagId) {
        noteNoteTagJpaRepository.findByNoteIdAndNoteTagId(noteId, tagId)
                .ifPresent(noteNoteTagJpaRepository::delete);
    }

    @Override
    public void removeAllTagsFromNote(UUID noteId) {
        noteNoteTagJpaRepository.deleteAllByNoteId(noteId);
    }

    @Override
    public List<NoteTag> findTagsByNoteId(UUID noteId) {
        return noteTagJpaRepository.findByNoteId(noteId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<UUID> findNoteIdsByTagId(UUID tagId) {
        return noteNoteTagJpaRepository.findByNoteTagId(tagId).stream()
                .map(NoteNoteTagEntity::getNoteId)
                .toList();
    }
}
