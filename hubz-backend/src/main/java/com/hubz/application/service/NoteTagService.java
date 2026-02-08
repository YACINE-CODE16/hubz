package com.hubz.application.service;

import com.hubz.application.dto.request.CreateNoteTagRequest;
import com.hubz.application.dto.request.UpdateNoteTagRequest;
import com.hubz.application.dto.response.NoteTagResponse;
import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.application.port.out.NoteTagRepositoryPort;
import com.hubz.domain.exception.NoteNotFoundException;
import com.hubz.domain.exception.NoteTagNotFoundException;
import com.hubz.domain.model.Note;
import com.hubz.domain.model.NoteTag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteTagService {

    private final NoteTagRepositoryPort noteTagRepository;
    private final NoteRepositoryPort noteRepository;
    private final AuthorizationService authorizationService;

    @Transactional
    public NoteTagResponse create(CreateNoteTagRequest request, UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        NoteTag tag = NoteTag.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .color(request.getColor())
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(noteTagRepository.save(tag));
    }

    @Transactional(readOnly = true)
    public List<NoteTagResponse> getByOrganization(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        return noteTagRepository.findByOrganizationId(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NoteTagResponse getById(UUID id, UUID currentUserId) {
        NoteTag tag = noteTagRepository.findById(id)
                .orElseThrow(() -> new NoteTagNotFoundException(id));

        authorizationService.checkOrganizationAccess(tag.getOrganizationId(), currentUserId);

        return toResponse(tag);
    }

    @Transactional
    public NoteTagResponse update(UUID id, UpdateNoteTagRequest request, UUID currentUserId) {
        NoteTag tag = noteTagRepository.findById(id)
                .orElseThrow(() -> new NoteTagNotFoundException(id));

        authorizationService.checkOrganizationAccess(tag.getOrganizationId(), currentUserId);

        if (request.getName() != null) {
            tag.setName(request.getName());
        }
        if (request.getColor() != null) {
            tag.setColor(request.getColor());
        }

        return toResponse(noteTagRepository.save(tag));
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        NoteTag tag = noteTagRepository.findById(id)
                .orElseThrow(() -> new NoteTagNotFoundException(id));

        authorizationService.checkOrganizationAccess(tag.getOrganizationId(), currentUserId);

        noteTagRepository.deleteById(id);
    }

    // Note-Tag operations

    @Transactional
    public void addTagToNote(UUID noteId, UUID tagId, UUID currentUserId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        NoteTag tag = noteTagRepository.findById(tagId)
                .orElseThrow(() -> new NoteTagNotFoundException(tagId));

        // Verify both belong to the same organization
        if (!note.getOrganizationId().equals(tag.getOrganizationId())) {
            throw new IllegalArgumentException("Tag and note must belong to the same organization");
        }

        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        noteTagRepository.addTagToNote(noteId, tagId);
    }

    @Transactional
    public void removeTagFromNote(UUID noteId, UUID tagId, UUID currentUserId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        noteTagRepository.removeTagFromNote(noteId, tagId);
    }

    @Transactional
    public void setNoteTags(UUID noteId, Set<UUID> tagIds, UUID currentUserId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        // Verify all tags belong to the same organization
        if (!tagIds.isEmpty()) {
            List<NoteTag> tags = noteTagRepository.findByIds(tagIds);
            for (NoteTag tag : tags) {
                if (!tag.getOrganizationId().equals(note.getOrganizationId())) {
                    throw new IllegalArgumentException("All tags must belong to the same organization as the note");
                }
            }
        }

        // Remove all existing tags and add new ones
        noteTagRepository.removeAllTagsFromNote(noteId);
        for (UUID tagId : tagIds) {
            noteTagRepository.addTagToNote(noteId, tagId);
        }
    }

    @Transactional(readOnly = true)
    public List<NoteTagResponse> getTagsByNote(UUID noteId, UUID currentUserId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        return noteTagRepository.findTagsByNoteId(noteId).stream()
                .map(this::toResponse)
                .toList();
    }

    private NoteTagResponse toResponse(NoteTag tag) {
        return NoteTagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .organizationId(tag.getOrganizationId())
                .createdAt(tag.getCreatedAt())
                .build();
    }
}
