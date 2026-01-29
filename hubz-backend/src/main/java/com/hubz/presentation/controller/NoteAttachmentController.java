package com.hubz.presentation.controller;

import com.hubz.application.dto.response.NoteAttachmentResponse;
import com.hubz.application.port.out.NoteAttachmentRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.NoteAttachmentService;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.NoteAttachment;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NoteAttachmentController {

    private final NoteAttachmentService attachmentService;
    private final NoteAttachmentRepositoryPort attachmentRepository;
    private final UserRepositoryPort userRepositoryPort;

    @PostMapping("/api/notes/{noteId}/attachments")
    public ResponseEntity<NoteAttachmentResponse> uploadAttachment(
            @PathVariable UUID noteId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        UUID userId = resolveUserId(authentication);
        NoteAttachmentResponse response = attachmentService.uploadAttachment(noteId, file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/notes/{noteId}/attachments")
    public ResponseEntity<List<NoteAttachmentResponse>> getAttachments(
            @PathVariable UUID noteId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        List<NoteAttachmentResponse> attachments = attachmentService.getAttachments(noteId, userId);
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/api/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable UUID attachmentId,
            Authentication authentication) throws MalformedURLException {
        UUID userId = resolveUserId(authentication);

        Resource resource = attachmentService.downloadAttachment(attachmentId, userId);

        // Get attachment metadata for filename
        NoteAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType() != null
                        ? attachment.getContentType()
                        : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getOriginalFileName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/api/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID attachmentId,
            Authentication authentication) throws IOException {
        UUID userId = resolveUserId(authentication);
        attachmentService.deleteAttachment(attachmentId, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
