package com.hubz.application.service;

import com.hubz.application.dto.request.CreateNoteRequest;
import com.hubz.application.dto.request.UpdateNoteRequest;
import com.hubz.application.dto.response.NoteResponse;
import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.domain.exception.NoteNotFoundException;
import com.hubz.domain.model.Note;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepositoryPort noteRepository;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<NoteResponse> getByOrganization(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);
        return noteRepository.findByOrganizationId(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> getByOrganizationAndCategory(
            UUID organizationId,
            String category,
            UUID currentUserId
    ) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);
        return noteRepository.findByOrganizationIdAndCategory(organizationId, category).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NoteResponse create(CreateNoteRequest request, UUID organizationId, UUID userId) {
        authorizationService.checkOrganizationAccess(organizationId, userId);

        Note note = Note.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .organizationId(organizationId)
                .createdById(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(noteRepository.save(note));
    }

    @Transactional
    public NoteResponse update(UUID id, UpdateNoteRequest request, UUID currentUserId) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));

        // Check access: org member
        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setCategory(request.getCategory());
        note.setUpdatedAt(LocalDateTime.now());

        return toResponse(noteRepository.save(note));
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));

        // Check access: org member
        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        noteRepository.delete(note);
    }

    private NoteResponse toResponse(Note note) {
        return NoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .category(note.getCategory())
                .organizationId(note.getOrganizationId())
                .createdById(note.getCreatedById())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
