package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.domain.model.Note;
import com.hubz.infrastructure.persistence.entity.NoteEntity;
import com.hubz.infrastructure.persistence.mapper.NoteMapper;
import com.hubz.infrastructure.persistence.repository.NoteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NoteRepositoryAdapter implements NoteRepositoryPort {

    private final NoteJpaRepository jpaRepository;
    private final NoteMapper mapper;

    @Override
    public Note save(Note note) {
        NoteEntity entity = mapper.toEntity(note);
        NoteEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Note> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Note> findByOrganizationId(UUID organizationId) {
        return jpaRepository.findByOrganizationIdOrderByUpdatedAtDesc(organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Note> findByOrganizationIdAndCategory(UUID organizationId, String category) {
        return jpaRepository.findByOrganizationIdAndCategoryOrderByUpdatedAtDesc(organizationId, category).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(Note note) {
        jpaRepository.deleteById(note.getId());
    }
}
