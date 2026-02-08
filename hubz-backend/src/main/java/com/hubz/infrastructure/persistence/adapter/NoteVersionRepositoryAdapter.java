package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.NoteVersionRepositoryPort;
import com.hubz.domain.model.NoteVersion;
import com.hubz.infrastructure.persistence.entity.NoteVersionEntity;
import com.hubz.infrastructure.persistence.mapper.NoteVersionMapper;
import com.hubz.infrastructure.persistence.repository.NoteVersionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NoteVersionRepositoryAdapter implements NoteVersionRepositoryPort {

    private final NoteVersionJpaRepository jpaRepository;
    private final NoteVersionMapper mapper;

    @Override
    public NoteVersion save(NoteVersion noteVersion) {
        NoteVersionEntity entity = mapper.toEntity(noteVersion);
        NoteVersionEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<NoteVersion> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<NoteVersion> findByNoteIdOrderByVersionNumberDesc(UUID noteId) {
        return jpaRepository.findByNoteIdOrderByVersionNumberDesc(noteId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<NoteVersion> findLatestByNoteId(UUID noteId) {
        return jpaRepository.findLatestByNoteId(noteId).map(mapper::toDomain);
    }

    @Override
    public Optional<Integer> findMaxVersionNumberByNoteId(UUID noteId) {
        return jpaRepository.findMaxVersionNumberByNoteId(noteId);
    }

    @Override
    public void deleteByNoteId(UUID noteId) {
        jpaRepository.deleteByNoteId(noteId);
    }
}
