package com.hubz.application.service;

import com.hubz.application.dto.response.TaskAttachmentResponse;
import com.hubz.application.port.out.TaskAttachmentRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.exception.TaskAttachmentNotFoundException;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.model.Task;
import com.hubz.domain.model.TaskAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskAttachmentServiceTest {

    @Mock
    private TaskAttachmentRepositoryPort attachmentRepository;

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private TaskAttachmentService taskAttachmentService;

    private UUID taskId;
    private UUID organizationId;
    private UUID userId;
    private Task task;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();

        task = Task.builder()
                .id(taskId)
                .title("Test Task")
                .status(TaskStatus.TODO)
                .organizationId(organizationId)
                .creatorId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldUploadAttachment() throws IOException {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("document.pdf");
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");

        String storedFilePath = "task-attachments/" + taskId + "/abc123.pdf";
        TaskAttachment savedAttachment = TaskAttachment.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .fileName("abc123.pdf")
                .originalFileName("document.pdf")
                .filePath(storedFilePath)
                .fileSize(1024L)
                .contentType("application/pdf")
                .uploadedBy(userId)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
        when(fileStorageService.storeTaskAttachment(file, taskId)).thenReturn(storedFilePath);
        when(attachmentRepository.save(any(TaskAttachment.class))).thenReturn(savedAttachment);

        // When
        TaskAttachmentResponse response = taskAttachmentService.uploadAttachment(taskId, file, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOriginalFileName()).isEqualTo("document.pdf");
        assertThat(response.getFileSize()).isEqualTo(1024L);
        assertThat(response.getContentType()).isEqualTo("application/pdf");
        verify(fileStorageService).storeTaskAttachment(file, taskId);
        verify(attachmentRepository).save(any(TaskAttachment.class));
    }

    @Test
    void shouldThrowExceptionWhenTaskNotFoundOnUpload() throws IOException {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskAttachmentService.uploadAttachment(taskId, file, userId))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void shouldGetAttachments() {
        // Given
        TaskAttachment attachment1 = TaskAttachment.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .fileName("file1.pdf")
                .originalFileName("document1.pdf")
                .filePath("task-attachments/" + taskId + "/file1.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .uploadedBy(userId)
                .uploadedAt(LocalDateTime.now())
                .build();

        TaskAttachment attachment2 = TaskAttachment.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .fileName("file2.png")
                .originalFileName("image.png")
                .filePath("task-attachments/" + taskId + "/file2.png")
                .fileSize(2048L)
                .contentType("image/png")
                .uploadedBy(userId)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
        when(attachmentRepository.findByTaskId(taskId)).thenReturn(Arrays.asList(attachment1, attachment2));

        // When
        List<TaskAttachmentResponse> response = taskAttachmentService.getAttachments(taskId, userId);

        // Then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getOriginalFileName()).isEqualTo("document1.pdf");
        assertThat(response.get(1).getOriginalFileName()).isEqualTo("image.png");
    }

    @Test
    void shouldThrowExceptionWhenTaskNotFoundOnGetAttachments() {
        // Given
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskAttachmentService.getAttachments(taskId, userId))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void shouldThrowExceptionWhenDownloadingNonExistentAttachment() {
        // Given
        UUID attachmentId = UUID.randomUUID();
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskAttachmentService.downloadAttachment(attachmentId, userId))
                .isInstanceOf(TaskAttachmentNotFoundException.class);
    }

    @Test
    void shouldThrowExceptionWhenAttachmentNotFoundOnDownload() {
        // Given
        UUID attachmentId = UUID.randomUUID();
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskAttachmentService.downloadAttachment(attachmentId, userId))
                .isInstanceOf(TaskAttachmentNotFoundException.class);
    }

    @Test
    void shouldDeleteAttachment() throws IOException {
        // Given
        UUID attachmentId = UUID.randomUUID();
        String filePath = "task-attachments/" + taskId + "/file.pdf";
        TaskAttachment attachment = TaskAttachment.builder()
                .id(attachmentId)
                .taskId(taskId)
                .fileName("file.pdf")
                .originalFileName("document.pdf")
                .filePath(filePath)
                .fileSize(1024L)
                .contentType("application/pdf")
                .uploadedBy(userId)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
        doNothing().when(fileStorageService).deleteFile(filePath);
        doNothing().when(attachmentRepository).deleteById(attachmentId);

        // When
        taskAttachmentService.deleteAttachment(attachmentId, userId);

        // Then
        verify(fileStorageService).deleteFile(filePath);
        verify(attachmentRepository).deleteById(attachmentId);
    }

    @Test
    void shouldThrowExceptionWhenAttachmentNotFoundOnDelete() {
        // Given
        UUID attachmentId = UUID.randomUUID();
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskAttachmentService.deleteAttachment(attachmentId, userId))
                .isInstanceOf(TaskAttachmentNotFoundException.class);
    }

    @Test
    void shouldGetAttachmentById() {
        // Given
        UUID attachmentId = UUID.randomUUID();
        TaskAttachment attachment = TaskAttachment.builder()
                .id(attachmentId)
                .taskId(taskId)
                .fileName("file.pdf")
                .originalFileName("document.pdf")
                .filePath("task-attachments/" + taskId + "/file.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .uploadedBy(userId)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        // When
        TaskAttachment result = taskAttachmentService.getAttachmentById(attachmentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(attachmentId);
        assertThat(result.getOriginalFileName()).isEqualTo("document.pdf");
    }

    @Test
    void shouldThrowExceptionWhenAttachmentNotFoundOnGetById() {
        // Given
        UUID attachmentId = UUID.randomUUID();
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskAttachmentService.getAttachmentById(attachmentId))
                .isInstanceOf(TaskAttachmentNotFoundException.class);
    }

    @Test
    void shouldGetAttachmentCount() {
        // Given
        when(attachmentRepository.countByTaskId(taskId)).thenReturn(5);

        // When
        int count = taskAttachmentService.getAttachmentCount(taskId);

        // Then
        assertThat(count).isEqualTo(5);
    }
}
