package com.hubz.application.service;

import com.hubz.application.dto.response.DocumentPreviewResponse;
import com.hubz.application.dto.response.DocumentVersionResponse;
import com.hubz.application.dto.response.OrganizationDocumentResponse;
import com.hubz.application.port.out.DocumentVersionRepositoryPort;
import com.hubz.application.port.out.OrganizationDocumentRepositoryPort;
import com.hubz.application.port.out.TagRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.model.DocumentVersion;
import com.hubz.domain.model.OrganizationDocument;
import com.hubz.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationDocumentService Unit Tests")
class OrganizationDocumentServiceTest {

    @Mock
    private OrganizationDocumentRepositoryPort documentRepository;

    @Mock
    private DocumentVersionRepositoryPort versionRepository;

    @Mock
    private TagRepositoryPort tagRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private OrganizationDocumentService organizationDocumentService;

    private UUID organizationId;
    private UUID userId;
    private UUID documentId;
    private OrganizationDocument testDocument;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        documentId = UUID.randomUUID();

        testDocument = OrganizationDocument.builder()
                .id(documentId)
                .organizationId(organizationId)
                .fileName("stored-file.pdf")
                .originalFileName("report.pdf")
                .filePath("organizations/" + organizationId + "/stored-file.pdf")
                .fileSize(2048L)
                .contentType("application/pdf")
                .uploadedBy(userId)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Upload Document Tests")
    class UploadDocumentTests {

        @Test
        @DisplayName("Should successfully upload document")
        void shouldUploadDocument() throws IOException {
            // Given
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("report.pdf");
            when(file.getSize()).thenReturn(2048L);
            when(file.getContentType()).thenReturn("application/pdf");

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(fileStorageService.storeFile(file, organizationId)).thenReturn("organizations/" + organizationId + "/stored-file.pdf");
            when(documentRepository.save(any(OrganizationDocument.class))).thenReturn(testDocument);
            when(tagRepository.findTagsByDocumentId(any(UUID.class))).thenReturn(List.of());
            when(versionRepository.findByDocumentId(any(UUID.class))).thenReturn(List.of());

            // When
            OrganizationDocumentResponse response = organizationDocumentService.uploadDocument(organizationId, file, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(documentId);
            assertThat(response.getOrganizationId()).isEqualTo(organizationId);
            assertThat(response.getOriginalFileName()).isEqualTo("report.pdf");
            assertThat(response.getFileSize()).isEqualTo(2048L);
            assertThat(response.getContentType()).isEqualTo("application/pdf");
            assertThat(response.getTags()).isEmpty();
            assertThat(response.getCurrentVersionNumber()).isEqualTo(1);
            assertThat(response.getTotalVersions()).isEqualTo(1);

            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(fileStorageService).storeFile(file, organizationId);
            verify(documentRepository).save(any(OrganizationDocument.class));
        }

        @Test
        @DisplayName("Should save document with correct metadata")
        void shouldSaveDocumentWithCorrectMetadata() throws IOException {
            // Given
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("spreadsheet.xlsx");
            when(file.getSize()).thenReturn(4096L);
            when(file.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(fileStorageService.storeFile(file, organizationId)).thenReturn("organizations/" + organizationId + "/stored-spreadsheet.xlsx");
            when(tagRepository.findTagsByDocumentId(any(UUID.class))).thenReturn(List.of());
            when(versionRepository.findByDocumentId(any(UUID.class))).thenReturn(List.of());

            ArgumentCaptor<OrganizationDocument> captor = ArgumentCaptor.forClass(OrganizationDocument.class);
            when(documentRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

            // When
            organizationDocumentService.uploadDocument(organizationId, file, userId);

            // Then
            OrganizationDocument saved = captor.getValue();
            assertThat(saved.getOrganizationId()).isEqualTo(organizationId);
            assertThat(saved.getOriginalFileName()).isEqualTo("spreadsheet.xlsx");
            assertThat(saved.getFileSize()).isEqualTo(4096L);
            assertThat(saved.getUploadedBy()).isEqualTo(userId);
            assertThat(saved.getUploadedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when file storage fails")
        void shouldThrowExceptionWhenFileStorageFails() throws IOException {
            // Given
            MultipartFile file = mock(MultipartFile.class);
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(fileStorageService.storeFile(file, organizationId)).thenThrow(new IOException("Storage error"));

            // When & Then
            assertThatThrownBy(() -> organizationDocumentService.uploadDocument(organizationId, file, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to upload document");

            verify(documentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Documents Tests")
    class GetDocumentsTests {

        @Test
        @DisplayName("Should return list of documents for organization")
        void shouldReturnListOfDocuments() {
            // Given
            UUID document2Id = UUID.randomUUID();
            OrganizationDocument document2 = OrganizationDocument.builder()
                    .id(document2Id)
                    .organizationId(organizationId)
                    .fileName("another-file.docx")
                    .originalFileName("memo.docx")
                    .filePath("organizations/" + organizationId + "/another-file.docx")
                    .fileSize(1024L)
                    .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(documentRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testDocument, document2));
            when(tagRepository.findTagsByDocumentId(documentId)).thenReturn(List.of());
            when(tagRepository.findTagsByDocumentId(document2Id)).thenReturn(List.of());
            when(versionRepository.findByDocumentId(any(UUID.class))).thenReturn(List.of());

            // When
            List<OrganizationDocumentResponse> responses = organizationDocumentService.getDocuments(organizationId, userId);

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getOriginalFileName()).isEqualTo("report.pdf");
            assertThat(responses.get(1).getOriginalFileName()).isEqualTo("memo.docx");

            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
        }

        @Test
        @DisplayName("Should return empty list when no documents")
        void shouldReturnEmptyListWhenNoDocuments() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(documentRepository.findByOrganizationId(organizationId)).thenReturn(List.of());

            // When
            List<OrganizationDocumentResponse> responses = organizationDocumentService.getDocuments(organizationId, userId);

            // Then
            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Document Tests")
    class GetDocumentTests {

        @Test
        @DisplayName("Should return document by id")
        void shouldReturnDocumentById() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            OrganizationDocument result = organizationDocumentService.getDocument(documentId, userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(documentId);
            assertThat(result.getOriginalFileName()).isEqualTo("report.pdf");

            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
        }

        @Test
        @DisplayName("Should throw exception when document not found")
        void shouldThrowExceptionWhenDocumentNotFound() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> organizationDocumentService.getDocument(documentId, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Document not found");
        }
    }

    @Nested
    @DisplayName("Delete Document Tests")
    class DeleteDocumentTests {

        @Test
        @DisplayName("Should delete document successfully")
        void shouldDeleteDocument() throws IOException {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doNothing().when(fileStorageService).deleteFile(testDocument.getFilePath());
            when(versionRepository.findByDocumentId(documentId)).thenReturn(List.of());
            doNothing().when(versionRepository).deleteByDocumentId(documentId);
            doNothing().when(tagRepository).removeAllTagsFromDocument(documentId);
            doNothing().when(documentRepository).deleteById(documentId);

            // When
            organizationDocumentService.deleteDocument(documentId, userId);

            // Then
            verify(fileStorageService).deleteFile(testDocument.getFilePath());
            verify(versionRepository).deleteByDocumentId(documentId);
            verify(tagRepository).removeAllTagsFromDocument(documentId);
            verify(documentRepository).deleteById(documentId);
        }

        @Test
        @DisplayName("Should throw exception when document not found")
        void shouldThrowExceptionWhenDocumentNotFound() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> organizationDocumentService.deleteDocument(documentId, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Document not found");

            verify(documentRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw exception when file deletion fails")
        void shouldThrowExceptionWhenFileDeletionFails() throws IOException {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doThrow(new IOException("Delete error")).when(fileStorageService).deleteFile(testDocument.getFilePath());

            // When & Then
            assertThatThrownBy(() -> organizationDocumentService.deleteDocument(documentId, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to delete document file");

            verify(documentRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should delete document with versions")
        void shouldDeleteDocumentWithVersions() throws IOException {
            // Given
            DocumentVersion version1 = DocumentVersion.builder()
                    .id(UUID.randomUUID())
                    .documentId(documentId)
                    .versionNumber(1)
                    .filePath("version1-path")
                    .build();
            DocumentVersion version2 = DocumentVersion.builder()
                    .id(UUID.randomUUID())
                    .documentId(documentId)
                    .versionNumber(2)
                    .filePath("version2-path")
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doNothing().when(fileStorageService).deleteFile(any(String.class));
            when(versionRepository.findByDocumentId(documentId)).thenReturn(List.of(version1, version2));
            doNothing().when(versionRepository).deleteByDocumentId(documentId);
            doNothing().when(tagRepository).removeAllTagsFromDocument(documentId);
            doNothing().when(documentRepository).deleteById(documentId);

            // When
            organizationDocumentService.deleteDocument(documentId, userId);

            // Then
            verify(fileStorageService).deleteFile(testDocument.getFilePath());
            verify(fileStorageService).deleteFile("version1-path");
            verify(fileStorageService).deleteFile("version2-path");
            verify(versionRepository).deleteByDocumentId(documentId);
            verify(documentRepository).deleteById(documentId);
        }
    }

    @Nested
    @DisplayName("Document Preview Tests")
    class DocumentPreviewTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Should return preview for PDF document")
        void shouldReturnPreviewForPdfDocument() {
            // Given
            OrganizationDocument pdfDocument = OrganizationDocument.builder()
                    .id(documentId)
                    .organizationId(organizationId)
                    .fileName("stored-file.pdf")
                    .originalFileName("report.pdf")
                    .filePath("organizations/" + organizationId + "/stored-file.pdf")
                    .fileSize(2048L)
                    .contentType("application/pdf")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(pdfDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            DocumentPreviewResponse response = organizationDocumentService.getDocumentPreview(documentId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(documentId);
            assertThat(response.getOriginalFileName()).isEqualTo("report.pdf");
            assertThat(response.getContentType()).isEqualTo("application/pdf");
            assertThat(response.isPreviewable()).isTrue();
            assertThat(response.getPreviewType()).isEqualTo("pdf");
            assertThat(response.getTextContent()).isNull();
        }

        @Test
        @DisplayName("Should return preview for image document")
        void shouldReturnPreviewForImageDocument() {
            // Given
            OrganizationDocument imageDocument = OrganizationDocument.builder()
                    .id(documentId)
                    .organizationId(organizationId)
                    .fileName("stored-image.png")
                    .originalFileName("screenshot.png")
                    .filePath("organizations/" + organizationId + "/stored-image.png")
                    .fileSize(1024L)
                    .contentType("image/png")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(imageDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            DocumentPreviewResponse response = organizationDocumentService.getDocumentPreview(documentId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isPreviewable()).isTrue();
            assertThat(response.getPreviewType()).isEqualTo("image");
        }

        @Test
        @DisplayName("Should return preview with text content for text document")
        void shouldReturnPreviewWithTextContentForTextDocument() throws IOException {
            // Given
            String textContent = "Hello, this is a test file content.";
            Path textFile = tempDir.resolve("test.txt");
            Files.writeString(textFile, textContent);

            OrganizationDocument textDocument = OrganizationDocument.builder()
                    .id(documentId)
                    .organizationId(organizationId)
                    .fileName("stored-text.txt")
                    .originalFileName("notes.txt")
                    .filePath("text-file-path")
                    .fileSize((long) textContent.length())
                    .contentType("text/plain")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(textDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(fileStorageService.getFilePath("text-file-path")).thenReturn(textFile);

            // When
            DocumentPreviewResponse response = organizationDocumentService.getDocumentPreview(documentId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isPreviewable()).isTrue();
            assertThat(response.getPreviewType()).isEqualTo("text");
            assertThat(response.getTextContent()).isEqualTo(textContent);
        }

        @Test
        @DisplayName("Should return unsupported preview for unsupported file type")
        void shouldReturnUnsupportedPreviewForUnsupportedFileType() {
            // Given
            OrganizationDocument zipDocument = OrganizationDocument.builder()
                    .id(documentId)
                    .organizationId(organizationId)
                    .fileName("stored-archive.zip")
                    .originalFileName("archive.zip")
                    .filePath("organizations/" + organizationId + "/stored-archive.zip")
                    .fileSize(5000L)
                    .contentType("application/zip")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(zipDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            DocumentPreviewResponse response = organizationDocumentService.getDocumentPreview(documentId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isPreviewable()).isFalse();
            assertThat(response.getPreviewType()).isEqualTo("unsupported");
        }

        @Test
        @DisplayName("Should recognize text file by extension when content type is generic")
        void shouldRecognizeTextFileByExtension() {
            // Given
            OrganizationDocument jsonDocument = OrganizationDocument.builder()
                    .id(documentId)
                    .organizationId(organizationId)
                    .fileName("stored-config.json")
                    .originalFileName("config.json")
                    .filePath("organizations/" + organizationId + "/stored-config.json")
                    .fileSize(500L)
                    .contentType("application/octet-stream") // Generic type
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(jsonDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            DocumentPreviewResponse response = organizationDocumentService.getDocumentPreview(documentId, userId);

            // Then
            // Note: application/octet-stream doesn't match text types, but .json extension should
            // Actually, the implementation checks contentType first, then extension
            // Since application/octet-stream is not in textTypes, it will check extension
            assertThat(response).isNotNull();
            assertThat(response.getPreviewType()).isEqualTo("text");
            assertThat(response.isPreviewable()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when document not found for preview")
        void shouldThrowExceptionWhenDocumentNotFoundForPreview() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> organizationDocumentService.getDocumentPreview(documentId, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Document not found");
        }

        @Test
        @DisplayName("Should return preview for JPEG image")
        void shouldReturnPreviewForJpegImage() {
            // Given
            OrganizationDocument jpegDocument = OrganizationDocument.builder()
                    .id(documentId)
                    .organizationId(organizationId)
                    .fileName("photo.jpg")
                    .originalFileName("photo.jpg")
                    .filePath("organizations/" + organizationId + "/photo.jpg")
                    .fileSize(3000L)
                    .contentType("image/jpeg")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(jpegDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            DocumentPreviewResponse response = organizationDocumentService.getDocumentPreview(documentId, userId);

            // Then
            assertThat(response.getPreviewType()).isEqualTo("image");
            assertThat(response.isPreviewable()).isTrue();
        }

        @Test
        @DisplayName("Should return preview for GIF image")
        void shouldReturnPreviewForGifImage() {
            // Given
            OrganizationDocument gifDocument = OrganizationDocument.builder()
                    .id(documentId)
                    .organizationId(organizationId)
                    .fileName("animation.gif")
                    .originalFileName("animation.gif")
                    .filePath("organizations/" + organizationId + "/animation.gif")
                    .fileSize(2000L)
                    .contentType("image/gif")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(gifDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            DocumentPreviewResponse response = organizationDocumentService.getDocumentPreview(documentId, userId);

            // Then
            assertThat(response.getPreviewType()).isEqualTo("image");
            assertThat(response.isPreviewable()).isTrue();
        }

        @Test
        @DisplayName("Should return preview for WebP image")
        void shouldReturnPreviewForWebpImage() {
            // Given
            OrganizationDocument webpDocument = OrganizationDocument.builder()
                    .id(documentId)
                    .organizationId(organizationId)
                    .fileName("image.webp")
                    .originalFileName("image.webp")
                    .filePath("organizations/" + organizationId + "/image.webp")
                    .fileSize(1500L)
                    .contentType("image/webp")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(webpDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            DocumentPreviewResponse response = organizationDocumentService.getDocumentPreview(documentId, userId);

            // Then
            assertThat(response.getPreviewType()).isEqualTo("image");
            assertThat(response.isPreviewable()).isTrue();
        }

        @Test
        @DisplayName("Should return preview for SVG image")
        void shouldReturnPreviewForSvgImage() {
            // Given
            OrganizationDocument svgDocument = OrganizationDocument.builder()
                    .id(documentId)
                    .organizationId(organizationId)
                    .fileName("icon.svg")
                    .originalFileName("icon.svg")
                    .filePath("organizations/" + organizationId + "/icon.svg")
                    .fileSize(800L)
                    .contentType("image/svg+xml")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(svgDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            DocumentPreviewResponse response = organizationDocumentService.getDocumentPreview(documentId, userId);

            // Then
            assertThat(response.getPreviewType()).isEqualTo("image");
            assertThat(response.isPreviewable()).isTrue();
        }

        @Test
        @DisplayName("Should return error message when text file not found")
        void shouldReturnErrorMessageWhenTextFileNotFound() {
            // Given
            Path nonExistentPath = tempDir.resolve("non-existent.txt");

            OrganizationDocument textDocument = OrganizationDocument.builder()
                    .id(documentId)
                    .organizationId(organizationId)
                    .fileName("missing.txt")
                    .originalFileName("missing.txt")
                    .filePath("missing-path")
                    .fileSize(100L)
                    .contentType("text/plain")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(textDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(fileStorageService.getFilePath("missing-path")).thenReturn(nonExistentPath);

            // When
            DocumentPreviewResponse response = organizationDocumentService.getDocumentPreview(documentId, userId);

            // Then
            assertThat(response.getPreviewType()).isEqualTo("text");
            assertThat(response.getTextContent()).isEqualTo("[File not found]");
        }

        @Test
        @DisplayName("Should handle null content type gracefully")
        void shouldHandleNullContentTypeGracefully() {
            // Given
            OrganizationDocument unknownDocument = OrganizationDocument.builder()
                    .id(documentId)
                    .organizationId(organizationId)
                    .fileName("unknown-file")
                    .originalFileName("unknown-file")
                    .filePath("organizations/" + organizationId + "/unknown-file")
                    .fileSize(1000L)
                    .contentType(null)
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(unknownDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            DocumentPreviewResponse response = organizationDocumentService.getDocumentPreview(documentId, userId);

            // Then
            assertThat(response.getPreviewType()).isEqualTo("unsupported");
            assertThat(response.isPreviewable()).isFalse();
        }

        @Test
        @DisplayName("Should recognize markdown file by extension")
        void shouldRecognizeMarkdownFileByExtension() {
            // Given
            OrganizationDocument mdDocument = OrganizationDocument.builder()
                    .id(documentId)
                    .organizationId(organizationId)
                    .fileName("readme.md")
                    .originalFileName("README.md")
                    .filePath("organizations/" + organizationId + "/readme.md")
                    .fileSize(500L)
                    .contentType("application/octet-stream")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(mdDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            DocumentPreviewResponse response = organizationDocumentService.getDocumentPreview(documentId, userId);

            // Then
            assertThat(response.getPreviewType()).isEqualTo("text");
            assertThat(response.isPreviewable()).isTrue();
        }

        @Test
        @DisplayName("Should recognize Java file by extension")
        void shouldRecognizeJavaFileByExtension() {
            // Given
            OrganizationDocument javaDocument = OrganizationDocument.builder()
                    .id(documentId)
                    .organizationId(organizationId)
                    .fileName("Main.java")
                    .originalFileName("Main.java")
                    .filePath("organizations/" + organizationId + "/Main.java")
                    .fileSize(2000L)
                    .contentType("application/octet-stream")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(javaDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            DocumentPreviewResponse response = organizationDocumentService.getDocumentPreview(documentId, userId);

            // Then
            assertThat(response.getPreviewType()).isEqualTo("text");
            assertThat(response.isPreviewable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Document Versioning Tests")
    class DocumentVersioningTests {

        private User testUser;

        @BeforeEach
        void setUpVersioningTests() {
            testUser = User.builder()
                    .id(userId)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .build();
        }

        @Test
        @DisplayName("Should upload first version and preserve original as version 1")
        void shouldUploadFirstVersionAndPreserveOriginal() throws IOException {
            // Given
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("report-v2.pdf");
            when(file.getSize()).thenReturn(3000L);
            when(file.getContentType()).thenReturn("application/pdf");

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(versionRepository.findMaxVersionNumberByDocumentId(documentId)).thenReturn(Optional.empty());
            when(fileStorageService.storeFile(file, organizationId)).thenReturn("new-file-path");
            when(versionRepository.save(any(DocumentVersion.class))).thenAnswer(i -> i.getArgument(0));
            when(documentRepository.save(any(OrganizationDocument.class))).thenAnswer(i -> i.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            DocumentVersionResponse response = organizationDocumentService.uploadNewVersion(documentId, file, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getVersionNumber()).isEqualTo(2);
            assertThat(response.getOriginalFileName()).isEqualTo("report-v2.pdf");

            // Verify original was saved as version 1
            ArgumentCaptor<DocumentVersion> versionCaptor = ArgumentCaptor.forClass(DocumentVersion.class);
            verify(versionRepository, times(2)).save(versionCaptor.capture());

            List<DocumentVersion> savedVersions = versionCaptor.getAllValues();
            assertThat(savedVersions.get(0).getVersionNumber()).isEqualTo(1); // Original
            assertThat(savedVersions.get(1).getVersionNumber()).isEqualTo(2); // New version
        }

        @Test
        @DisplayName("Should upload subsequent version with correct version number")
        void shouldUploadSubsequentVersionWithCorrectNumber() throws IOException {
            // Given
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("report-v3.pdf");
            when(file.getSize()).thenReturn(4000L);
            when(file.getContentType()).thenReturn("application/pdf");

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(versionRepository.findMaxVersionNumberByDocumentId(documentId)).thenReturn(Optional.of(2));
            when(fileStorageService.storeFile(file, organizationId)).thenReturn("new-file-path");
            when(versionRepository.save(any(DocumentVersion.class))).thenAnswer(i -> i.getArgument(0));
            when(documentRepository.save(any(OrganizationDocument.class))).thenAnswer(i -> i.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            DocumentVersionResponse response = organizationDocumentService.uploadNewVersion(documentId, file, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getVersionNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should get all versions of a document")
        void shouldGetAllVersionsOfDocument() {
            // Given
            DocumentVersion version1 = DocumentVersion.builder()
                    .id(UUID.randomUUID())
                    .documentId(documentId)
                    .versionNumber(1)
                    .fileName("v1-file.pdf")
                    .originalFileName("report.pdf")
                    .fileSize(2000L)
                    .contentType("application/pdf")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now().minusDays(2))
                    .build();

            DocumentVersion version2 = DocumentVersion.builder()
                    .id(UUID.randomUUID())
                    .documentId(documentId)
                    .versionNumber(2)
                    .fileName("v2-file.pdf")
                    .originalFileName("report-v2.pdf")
                    .fileSize(2500L)
                    .contentType("application/pdf")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(versionRepository.findByDocumentId(documentId)).thenReturn(List.of(version2, version1));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            List<DocumentVersionResponse> versions = organizationDocumentService.getDocumentVersions(documentId, userId);

            // Then
            assertThat(versions).hasSize(2);
            assertThat(versions.get(0).getVersionNumber()).isEqualTo(2);
            assertThat(versions.get(1).getVersionNumber()).isEqualTo(1);
            assertThat(versions.get(0).getUploadedByName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should return current document as version 1 when no versions exist")
        void shouldReturnCurrentDocumentAsVersion1WhenNoVersionsExist() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(versionRepository.findByDocumentId(documentId)).thenReturn(List.of());
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            List<DocumentVersionResponse> versions = organizationDocumentService.getDocumentVersions(documentId, userId);

            // Then
            assertThat(versions).hasSize(1);
            assertThat(versions.get(0).getVersionNumber()).isEqualTo(1);
            assertThat(versions.get(0).getOriginalFileName()).isEqualTo(testDocument.getOriginalFileName());
        }

        @Test
        @DisplayName("Should get specific version by version number")
        void shouldGetSpecificVersionByNumber() {
            // Given
            DocumentVersion version2 = DocumentVersion.builder()
                    .id(UUID.randomUUID())
                    .documentId(documentId)
                    .versionNumber(2)
                    .fileName("v2-file.pdf")
                    .originalFileName("report-v2.pdf")
                    .filePath("v2-path")
                    .fileSize(2500L)
                    .contentType("application/pdf")
                    .uploadedBy(userId)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(versionRepository.findByDocumentId(documentId)).thenReturn(List.of(version2));

            // When
            DocumentVersion result = organizationDocumentService.getDocumentVersionByNumber(documentId, 2, userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getVersionNumber()).isEqualTo(2);
            assertThat(result.getOriginalFileName()).isEqualTo("report-v2.pdf");
        }

        @Test
        @DisplayName("Should throw exception when version not found")
        void shouldThrowExceptionWhenVersionNotFound() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(versionRepository.findByDocumentId(documentId)).thenReturn(List.of());

            // When & Then
            assertThatThrownBy(() -> organizationDocumentService.getDocumentVersionByNumber(documentId, 5, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Version 5 not found");
        }

        @Test
        @DisplayName("Should update main document when uploading new version")
        void shouldUpdateMainDocumentWhenUploadingNewVersion() throws IOException {
            // Given
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("updated-report.pdf");
            when(file.getSize()).thenReturn(5000L);
            when(file.getContentType()).thenReturn("application/pdf");

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(versionRepository.findMaxVersionNumberByDocumentId(documentId)).thenReturn(Optional.of(1));
            when(fileStorageService.storeFile(file, organizationId)).thenReturn("updated-file-path");
            when(versionRepository.save(any(DocumentVersion.class))).thenAnswer(i -> i.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            ArgumentCaptor<OrganizationDocument> docCaptor = ArgumentCaptor.forClass(OrganizationDocument.class);
            when(documentRepository.save(docCaptor.capture())).thenAnswer(i -> i.getArgument(0));

            // When
            organizationDocumentService.uploadNewVersion(documentId, file, userId);

            // Then
            OrganizationDocument updatedDoc = docCaptor.getValue();
            assertThat(updatedDoc.getOriginalFileName()).isEqualTo("updated-report.pdf");
            assertThat(updatedDoc.getFileSize()).isEqualTo(5000L);
            assertThat(updatedDoc.getFilePath()).isEqualTo("updated-file-path");
        }

        @Test
        @DisplayName("Should throw exception when uploading version for non-existent document")
        void shouldThrowExceptionWhenUploadingVersionForNonExistentDocument() {
            // Given
            MultipartFile file = mock(MultipartFile.class);
            when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> organizationDocumentService.uploadNewVersion(documentId, file, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Document not found");
        }
    }
}
