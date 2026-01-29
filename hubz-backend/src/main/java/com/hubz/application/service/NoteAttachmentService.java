package com.hubz.application.service;

import com.hubz.application.dto.response.NoteAttachmentResponse;
import com.hubz.application.port.out.NoteAttachmentRepositoryPort;
import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.domain.exception.NoteNotFoundException;
import com.hubz.domain.model.Note;
import com.hubz.domain.model.NoteAttachment;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteAttachmentService {

    private final NoteAttachmentRepositoryPort attachmentRepository;
    private final NoteRepositoryPort noteRepository;
    private final FileStorageService fileStorageService;
    private final AuthorizationService authorizationService;

    @Transactional
    public NoteAttachmentResponse uploadAttachment(UUID noteId, MultipartFile file, UUID uploadedBy) throws IOException {
        // Verify note exists and user has access
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        authorizationService.checkOrganizationAccess(note.getOrganizationId(), uploadedBy);

        // Store file
        String filePath = fileStorageService.storeFile(file, noteId);

        // Save metadata
        NoteAttachment attachment = NoteAttachment.builder()
                .id(UUID.randomUUID())
                .noteId(noteId)
                .fileName(filePath.substring(filePath.lastIndexOf('/') + 1))
                .originalFileName(file.getOriginalFilename())
                .filePath(filePath)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .uploadedBy(uploadedBy)
                .uploadedAt(LocalDateTime.now())
                .build();

        NoteAttachment saved = attachmentRepository.save(attachment);
        return toResponse(saved);
    }

    public List<NoteAttachmentResponse> getAttachments(UUID noteId, UUID currentUserId) {
        // Verify note exists and user has access
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        return attachmentRepository.findByNoteId(noteId).stream()
                .map(this::toResponse)
                .toList();
    }

    public Resource downloadAttachment(UUID attachmentId, UUID currentUserId) throws MalformedURLException {
        NoteAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));

        // Verify access
        Note note = noteRepository.findById(attachment.getNoteId())
                .orElseThrow(() -> new NoteNotFoundException(attachment.getNoteId()));

        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        Path filePath = fileStorageService.getFilePath(attachment.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            throw new RuntimeException("File not found");
        }

        return resource;
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId, UUID currentUserId) throws IOException {
        NoteAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));

        // Verify access
        Note note = noteRepository.findById(attachment.getNoteId())
                .orElseThrow(() -> new NoteNotFoundException(attachment.getNoteId()));

        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        // Delete file
        fileStorageService.deleteFile(attachment.getFilePath());

        // Delete metadata
        attachmentRepository.deleteById(attachmentId);
    }

    private NoteAttachmentResponse toResponse(NoteAttachment attachment) {
        return NoteAttachmentResponse.builder()
                .id(attachment.getId())
                .noteId(attachment.getNoteId())
                .fileName(attachment.getFileName())
                .originalFileName(attachment.getOriginalFileName())
                .fileSize(attachment.getFileSize())
                .contentType(attachment.getContentType())
                .uploadedBy(attachment.getUploadedBy())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }
}
