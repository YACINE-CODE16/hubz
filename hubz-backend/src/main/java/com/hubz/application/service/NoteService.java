package com.hubz.application.service;

import com.hubz.application.dto.request.CreateNoteRequest;
import com.hubz.application.dto.request.UpdateNoteRequest;
import com.hubz.application.dto.response.NoteResponse;
import com.hubz.application.dto.response.NoteTagResponse;
import com.hubz.application.port.out.NoteFolderRepositoryPort;
import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.application.port.out.NoteTagRepositoryPort;
import com.hubz.domain.enums.WebhookEventType;
import com.hubz.domain.exception.NoteFolderNotFoundException;
import com.hubz.domain.exception.NoteNotFoundException;
import com.hubz.domain.model.Note;
import com.hubz.domain.model.NoteFolder;
import com.hubz.domain.model.NoteTag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepositoryPort noteRepository;
    private final NoteTagRepositoryPort noteTagRepository;
    private final NoteFolderRepositoryPort noteFolderRepository;
    private final NoteVersionService noteVersionService;
    private final AuthorizationService authorizationService;
    private final WebhookService webhookService;

    @Transactional(readOnly = true)
    public List<NoteResponse> getByOrganization(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);
        return noteRepository.findByOrganizationId(organizationId).stream()
                .map(this::toResponseWithTags)
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
                .map(this::toResponseWithTags)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> getByOrganizationAndFolder(
            UUID organizationId,
            UUID folderId,
            UUID currentUserId
    ) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        if (folderId != null) {
            // Verify folder exists and belongs to org
            var folder = noteFolderRepository.findById(folderId)
                    .orElseThrow(() -> new NoteFolderNotFoundException(folderId));
            if (!folder.getOrganizationId().equals(organizationId)) {
                throw new IllegalArgumentException("Folder does not belong to this organization");
            }
            return noteRepository.findByOrganizationIdAndFolderId(organizationId, folderId).stream()
                    .map(this::toResponseWithTags)
                    .toList();
        } else {
            // Get notes without folder (root level)
            return noteRepository.findByOrganizationIdAndFolderIdIsNull(organizationId).stream()
                    .map(this::toResponseWithTags)
                    .toList();
        }
    }

    @Transactional(readOnly = true)
    public NoteResponse getById(UUID id, UUID currentUserId) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));

        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        return toResponseWithTags(note);
    }

    @Transactional
    public NoteResponse create(CreateNoteRequest request, UUID organizationId, UUID userId) {
        authorizationService.checkOrganizationAccess(organizationId, userId);

        // Validate folder if provided
        if (request.getFolderId() != null) {
            NoteFolder folder = noteFolderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new NoteFolderNotFoundException(request.getFolderId()));
            if (!folder.getOrganizationId().equals(organizationId)) {
                throw new IllegalArgumentException("Folder does not belong to this organization");
            }
        }

        Note note = Note.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .folderId(request.getFolderId())
                .organizationId(organizationId)
                .createdById(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Note saved = noteRepository.save(note);

        // Add tags if provided
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            for (UUID tagId : request.getTagIds()) {
                noteTagRepository.addTagToNote(saved.getId(), tagId);
            }
        }

        // Send webhook event for note creation
        webhookService.handleWebhookEvent(organizationId, WebhookEventType.NOTE_CREATED, Map.of(
                "noteId", saved.getId().toString(),
                "title", saved.getTitle(),
                "category", saved.getCategory() != null ? saved.getCategory() : "",
                "createdById", userId.toString()
        ));

        return toResponseWithTags(saved);
    }

    @Transactional
    public NoteResponse update(UUID id, UpdateNoteRequest request, UUID currentUserId) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));

        // Check access: org member
        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        // Create a version of the current state before updating (if content changed)
        boolean contentChanged = !note.getTitle().equals(request.getTitle())
                || !note.getContent().equals(request.getContent());
        if (contentChanged) {
            noteVersionService.createVersion(note, currentUserId);
        }

        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setCategory(request.getCategory());

        // Handle folder change
        if (request.getFolderId() != null) {
            NoteFolder folder = noteFolderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new NoteFolderNotFoundException(request.getFolderId()));
            if (!folder.getOrganizationId().equals(note.getOrganizationId())) {
                throw new IllegalArgumentException("Folder does not belong to this organization");
            }
            note.setFolderId(request.getFolderId());
        } else {
            // Explicitly set to null (move to root)
            note.setFolderId(null);
        }

        note.setUpdatedAt(LocalDateTime.now());

        Note saved = noteRepository.save(note);

        // Update tags if provided
        if (request.getTagIds() != null) {
            noteTagRepository.removeAllTagsFromNote(saved.getId());
            for (UUID tagId : request.getTagIds()) {
                noteTagRepository.addTagToNote(saved.getId(), tagId);
            }
        }

        return toResponseWithTags(saved);
    }

    @Transactional
    public NoteResponse moveToFolder(UUID noteId, UUID folderId, UUID currentUserId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        if (folderId != null) {
            var folder = noteFolderRepository.findById(folderId)
                    .orElseThrow(() -> new NoteFolderNotFoundException(folderId));
            if (!folder.getOrganizationId().equals(note.getOrganizationId())) {
                throw new IllegalArgumentException("Folder does not belong to this organization");
            }
        }

        note.setFolderId(folderId);
        note.setUpdatedAt(LocalDateTime.now());

        return toResponseWithTags(noteRepository.save(note));
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));

        // Check access: org member
        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        // Remove all versions first
        noteVersionService.deleteVersionsByNoteId(id);

        // Remove all tags
        noteTagRepository.removeAllTagsFromNote(id);

        noteRepository.delete(note);
    }

    private NoteResponse toResponseWithTags(Note note) {
        List<NoteTagResponse> tags = noteTagRepository.findTagsByNoteId(note.getId()).stream()
                .map(this::toTagResponse)
                .toList();

        return NoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .category(note.getCategory())
                .folderId(note.getFolderId())
                .organizationId(note.getOrganizationId())
                .createdById(note.getCreatedById())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .tags(tags)
                .build();
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> searchNotes(UUID organizationId, String query, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        if (query == null || query.trim().isEmpty()) {
            return noteRepository.findByOrganizationId(organizationId).stream()
                    .map(this::toResponseWithTags)
                    .toList();
        }

        return noteRepository.searchByTitleOrContent(query.trim(), List.of(organizationId)).stream()
                .map(this::toResponseWithTags)
                .toList();
    }

    private NoteTagResponse toTagResponse(NoteTag tag) {
        return NoteTagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .organizationId(tag.getOrganizationId())
                .createdAt(tag.getCreatedAt())
                .build();
    }
}
