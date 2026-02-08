package com.hubz.application.service;

import com.hubz.application.dto.response.NoteAttachmentResponse;
import com.hubz.application.port.out.NoteAttachmentRepositoryPort;
import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.domain.exception.NoteNotFoundException;
import com.hubz.domain.model.Note;
import com.hubz.domain.model.NoteAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoteAttachmentService Unit Tests")
class NoteAttachmentServiceTest {

    @Mock
    private NoteAttachmentRepositoryPort attachmentRepository;

    @Mock
    private NoteRepositoryPort noteRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private NoteAttachmentService noteAttachmentService;

    private UUID noteId;
    private UUID organizationId;
    private UUID userId;
    private UUID attachmentId;
    private Note testNote;
    private NoteAttachment testAttachment;

    @BeforeEach
    void setUp() {
        noteId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        attachmentId = UUID.randomUUID();

        testNote = Note.builder()
                .id(noteId)
                .title("Test Note")
                .content("Test content")
                .organizationId(organizationId)
                .createdById(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testAttachment = NoteAttachment.builder()
                .id(attachmentId)
                .noteId(noteId)
                .fileName("stored-file.pdf")
                .originalFileName("original.pdf")
                .filePath("notes/" + noteId + "/stored-file.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .uploadedBy(userId)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Upload Attachment Tests")
    class UploadAttachmentTests {

        @Test
        @DisplayName("Should successfully upload attachment")
        void shouldUploadAttachment() throws IOException {
            // Given
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("original.pdf");
            when(file.getSize()).thenReturn(1024L);
            when(file.getContentType()).thenReturn("application/pdf");

            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(fileStorageService.storeFile(file, noteId)).thenReturn("notes/" + noteId + "/stored-file.pdf");
            when(attachmentRepository.save(any(NoteAttachment.class))).thenReturn(testAttachment);

            // When
            NoteAttachmentResponse response = noteAttachmentService.uploadAttachment(noteId, file, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(attachmentId);
            assertThat(response.getNoteId()).isEqualTo(noteId);
            assertThat(response.getOriginalFileName()).isEqualTo("original.pdf");
            assertThat(response.getFileSize()).isEqualTo(1024L);
            assertThat(response.getContentType()).isEqualTo("application/pdf");

            verify(noteRepository).findById(noteId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(fileStorageService).storeFile(file, noteId);
            verify(attachmentRepository).save(any(NoteAttachment.class));
        }

        @Test
        @DisplayName("Should throw exception when note not found")
        void shouldThrowExceptionWhenNoteNotFound() {
            // Given
            MultipartFile file = mock(MultipartFile.class);
            when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteAttachmentService.uploadAttachment(noteId, file, userId))
                    .isInstanceOf(NoteNotFoundException.class);

            verify(attachmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should save attachment with correct metadata")
        void shouldSaveAttachmentWithCorrectMetadata() throws IOException {
            // Given
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("document.docx");
            when(file.getSize()).thenReturn(2048L);
            when(file.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(fileStorageService.storeFile(file, noteId)).thenReturn("notes/" + noteId + "/stored-document.docx");

            ArgumentCaptor<NoteAttachment> captor = ArgumentCaptor.forClass(NoteAttachment.class);
            when(attachmentRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

            // When
            noteAttachmentService.uploadAttachment(noteId, file, userId);

            // Then
            NoteAttachment saved = captor.getValue();
            assertThat(saved.getNoteId()).isEqualTo(noteId);
            assertThat(saved.getOriginalFileName()).isEqualTo("document.docx");
            assertThat(saved.getFileSize()).isEqualTo(2048L);
            assertThat(saved.getUploadedBy()).isEqualTo(userId);
            assertThat(saved.getUploadedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Get Attachments Tests")
    class GetAttachmentsTests {

        @Test
        @DisplayName("Should return list of attachments for note")
        void shouldReturnListOfAttachments() {
            // Given
            NoteAttachment attachment1 = testAttachment;
            NoteAttachment attachment2 = NoteAttachment.builder()
                    .id(UUID.randomUUID())
                    .noteId(noteId)
                    .fileName("another-file.png")
                    .originalFileName("image.png")
                    .filePath("notes/" + noteId + "/another-file.png")
                    .fileSize(512L)
                    .contentType("image/png")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(attachmentRepository.findByNoteId(noteId)).thenReturn(List.of(attachment1, attachment2));

            // When
            List<NoteAttachmentResponse> responses = noteAttachmentService.getAttachments(noteId, userId);

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getOriginalFileName()).isEqualTo("original.pdf");
            assertThat(responses.get(1).getOriginalFileName()).isEqualTo("image.png");

            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
        }

        @Test
        @DisplayName("Should return empty list when no attachments")
        void shouldReturnEmptyListWhenNoAttachments() {
            // Given
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(attachmentRepository.findByNoteId(noteId)).thenReturn(List.of());

            // When
            List<NoteAttachmentResponse> responses = noteAttachmentService.getAttachments(noteId, userId);

            // Then
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when note not found")
        void shouldThrowExceptionWhenNoteNotFound() {
            // Given
            when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteAttachmentService.getAttachments(noteId, userId))
                    .isInstanceOf(NoteNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Download Attachment Tests")
    class DownloadAttachmentTests {

        @Test
        @DisplayName("Should throw exception when attachment not found")
        void shouldThrowExceptionWhenAttachmentNotFound() {
            // Given
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteAttachmentService.downloadAttachment(attachmentId, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Attachment not found");
        }

        @Test
        @DisplayName("Should throw exception when note not found")
        void shouldThrowExceptionWhenNoteNotFoundDuringDownload() {
            // Given
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(testAttachment));
            when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteAttachmentService.downloadAttachment(attachmentId, userId))
                    .isInstanceOf(NoteNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Attachment Tests")
    class DeleteAttachmentTests {

        @Test
        @DisplayName("Should delete attachment successfully")
        void shouldDeleteAttachment() throws IOException {
            // Given
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(testAttachment));
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(testNote));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doNothing().when(fileStorageService).deleteFile(testAttachment.getFilePath());
            doNothing().when(attachmentRepository).deleteById(attachmentId);

            // When
            noteAttachmentService.deleteAttachment(attachmentId, userId);

            // Then
            verify(fileStorageService).deleteFile(testAttachment.getFilePath());
            verify(attachmentRepository).deleteById(attachmentId);
        }

        @Test
        @DisplayName("Should throw exception when attachment not found")
        void shouldThrowExceptionWhenAttachmentNotFound() {
            // Given
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteAttachmentService.deleteAttachment(attachmentId, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Attachment not found");

            verify(attachmentRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw exception when note not found during deletion")
        void shouldThrowExceptionWhenNoteNotFoundDuringDeletion() {
            // Given
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(testAttachment));
            when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteAttachmentService.deleteAttachment(attachmentId, userId))
                    .isInstanceOf(NoteNotFoundException.class);

            verify(attachmentRepository, never()).deleteById(any());
        }
    }
}
