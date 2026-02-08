package com.hubz.application.service;

import com.hubz.application.dto.response.TaskAttachmentResponse;
import com.hubz.application.port.out.TaskAttachmentRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.exception.TaskAttachmentNotFoundException;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.model.Task;
import com.hubz.domain.model.TaskAttachment;
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
public class TaskAttachmentService {

    private final TaskAttachmentRepositoryPort attachmentRepository;
    private final TaskRepositoryPort taskRepository;
    private final FileStorageService fileStorageService;
    private final AuthorizationService authorizationService;

    @Transactional
    public TaskAttachmentResponse uploadAttachment(UUID taskId, MultipartFile file, UUID uploadedBy) throws IOException {
        // Verify task exists and user has access
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), uploadedBy);

        // Store file
        String filePath = fileStorageService.storeTaskAttachment(file, taskId);

        // Save metadata
        TaskAttachment attachment = TaskAttachment.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .fileName(filePath.substring(filePath.lastIndexOf('/') + 1))
                .originalFileName(file.getOriginalFilename())
                .filePath(filePath)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .uploadedBy(uploadedBy)
                .uploadedAt(LocalDateTime.now())
                .build();

        TaskAttachment saved = attachmentRepository.save(attachment);
        return toResponse(saved);
    }

    public List<TaskAttachmentResponse> getAttachments(UUID taskId, UUID currentUserId) {
        // Verify task exists and user has access
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        return attachmentRepository.findByTaskId(taskId).stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskAttachment getAttachmentById(UUID attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new TaskAttachmentNotFoundException(attachmentId));
    }

    public Resource downloadAttachment(UUID attachmentId, UUID currentUserId) throws MalformedURLException {
        TaskAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new TaskAttachmentNotFoundException(attachmentId));

        // Verify access
        Task task = taskRepository.findById(attachment.getTaskId())
                .orElseThrow(() -> new TaskNotFoundException(attachment.getTaskId()));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        Path filePath = fileStorageService.getFilePath(attachment.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            throw new RuntimeException("File not found");
        }

        return resource;
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId, UUID currentUserId) throws IOException {
        TaskAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new TaskAttachmentNotFoundException(attachmentId));

        // Verify access
        Task task = taskRepository.findById(attachment.getTaskId())
                .orElseThrow(() -> new TaskNotFoundException(attachment.getTaskId()));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        // Delete file
        fileStorageService.deleteFile(attachment.getFilePath());

        // Delete metadata
        attachmentRepository.deleteById(attachmentId);
    }

    public int getAttachmentCount(UUID taskId) {
        return attachmentRepository.countByTaskId(taskId);
    }

    private TaskAttachmentResponse toResponse(TaskAttachment attachment) {
        return TaskAttachmentResponse.builder()
                .id(attachment.getId())
                .taskId(attachment.getTaskId())
                .fileName(attachment.getFileName())
                .originalFileName(attachment.getOriginalFileName())
                .fileSize(attachment.getFileSize())
                .contentType(attachment.getContentType())
                .uploadedBy(attachment.getUploadedBy())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }
}
