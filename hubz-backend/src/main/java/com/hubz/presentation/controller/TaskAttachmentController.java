package com.hubz.presentation.controller;

import com.hubz.application.dto.response.TaskAttachmentResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.TaskAttachmentService;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.TaskAttachment;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaskAttachmentController {

    private final TaskAttachmentService attachmentService;
    private final UserRepositoryPort userRepositoryPort;

    @PostMapping("/api/tasks/{taskId}/attachments")
    public ResponseEntity<TaskAttachmentResponse> uploadAttachment(
            @PathVariable UUID taskId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) throws IOException {
        UUID userId = resolveUserId(authentication);
        TaskAttachmentResponse response = attachmentService.uploadAttachment(taskId, file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/tasks/{taskId}/attachments")
    public ResponseEntity<List<TaskAttachmentResponse>> getAttachments(
            @PathVariable UUID taskId,
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        List<TaskAttachmentResponse> attachments = attachmentService.getAttachments(taskId, userId);
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/api/tasks/{taskId}/attachments/count")
    public ResponseEntity<Map<String, Integer>> getAttachmentCount(
            @PathVariable UUID taskId,
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        // Verify access by calling getAttachments (which checks permissions)
        attachmentService.getAttachments(taskId, userId);
        int count = attachmentService.getAttachmentCount(taskId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/api/task-attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable UUID attachmentId,
            Authentication authentication
    ) throws MalformedURLException {
        UUID userId = resolveUserId(authentication);
        Resource resource = attachmentService.downloadAttachment(attachmentId, userId);

        // Get attachment metadata for filename
        TaskAttachment attachment = attachmentService.getAttachmentById(attachmentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType() != null
                        ? attachment.getContentType()
                        : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getOriginalFileName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/api/task-attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID attachmentId,
            Authentication authentication
    ) throws IOException {
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
