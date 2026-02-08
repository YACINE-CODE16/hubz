package com.hubz.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileStorageService Unit Tests")
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Nested
    @DisplayName("Store File")
    class StoreFileTests {

        @Test
        @DisplayName("Should store file and return path")
        void shouldStoreFileAndReturnPath() throws IOException {
            // Given
            UUID noteId = UUID.randomUUID();
            MultipartFile file = new MockMultipartFile(
                    "test-file",
                    "document.pdf",
                    "application/pdf",
                    "test content".getBytes()
            );

            // When
            String storedPath = fileStorageService.storeFile(file, noteId);

            // Then
            assertThat(storedPath).isNotNull();
            assertThat(storedPath).startsWith(noteId.toString() + "/");
            assertThat(storedPath).endsWith(".pdf");

            Path fullPath = fileStorageService.getFilePath(storedPath);
            assertThat(Files.exists(fullPath)).isTrue();
            assertThat(Files.readString(fullPath)).isEqualTo("test content");
        }

        @Test
        @DisplayName("Should create note directory if it does not exist")
        void shouldCreateNoteDirectoryIfNotExists() throws IOException {
            // Given
            UUID noteId = UUID.randomUUID();
            MultipartFile file = new MockMultipartFile(
                    "test-file",
                    "image.png",
                    "image/png",
                    "image content".getBytes()
            );

            // When
            fileStorageService.storeFile(file, noteId);

            // Then
            Path noteDir = tempDir.resolve(noteId.toString());
            assertThat(Files.exists(noteDir)).isTrue();
            assertThat(Files.isDirectory(noteDir)).isTrue();
        }

        @Test
        @DisplayName("Should preserve file extension")
        void shouldPreserveFileExtension() throws IOException {
            // Given
            UUID noteId = UUID.randomUUID();
            MultipartFile file = new MockMultipartFile(
                    "test-file",
                    "document.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "excel content".getBytes()
            );

            // When
            String storedPath = fileStorageService.storeFile(file, noteId);

            // Then
            assertThat(storedPath).endsWith(".xlsx");
        }

        @Test
        @DisplayName("Should handle file without extension")
        void shouldHandleFileWithoutExtension() throws IOException {
            // Given
            UUID noteId = UUID.randomUUID();
            MultipartFile file = new MockMultipartFile(
                    "test-file",
                    "README",
                    "text/plain",
                    "readme content".getBytes()
            );

            // When
            String storedPath = fileStorageService.storeFile(file, noteId);

            // Then
            assertThat(storedPath).isNotNull();
            assertThat(storedPath).startsWith(noteId.toString() + "/");
            assertThat(storedPath).doesNotContain("."); // UUID only, no extension
        }

        @Test
        @DisplayName("Should generate unique filename for each upload")
        void shouldGenerateUniqueFilenameForEachUpload() throws IOException {
            // Given
            UUID noteId = UUID.randomUUID();
            MultipartFile file1 = new MockMultipartFile(
                    "test-file",
                    "same-name.txt",
                    "text/plain",
                    "content 1".getBytes()
            );
            MultipartFile file2 = new MockMultipartFile(
                    "test-file",
                    "same-name.txt",
                    "text/plain",
                    "content 2".getBytes()
            );

            // When
            String storedPath1 = fileStorageService.storeFile(file1, noteId);
            String storedPath2 = fileStorageService.storeFile(file2, noteId);

            // Then
            assertThat(storedPath1).isNotEqualTo(storedPath2);

            // Both files should exist
            assertThat(Files.exists(fileStorageService.getFilePath(storedPath1))).isTrue();
            assertThat(Files.exists(fileStorageService.getFilePath(storedPath2))).isTrue();
        }
    }

    @Nested
    @DisplayName("Get File Path")
    class GetFilePathTests {

        @Test
        @DisplayName("Should return normalized path")
        void shouldReturnNormalizedPath() {
            // Given
            String relativePath = "some-id/file.txt";

            // When
            Path result = fileStorageService.getFilePath(relativePath);

            // Then
            assertThat(result).isAbsolute();
            assertThat(result.toString()).contains("some-id");
            assertThat(result.toString()).endsWith("file.txt");
        }

        @Test
        @DisplayName("Should handle paths with nested directories")
        void shouldHandlePathsWithNestedDirectories() {
            // Given
            String relativePath = "dir1/dir2/file.txt";

            // When
            Path result = fileStorageService.getFilePath(relativePath);

            // Then
            assertThat(result).isAbsolute();
            assertThat(result.toString()).contains("dir1");
            assertThat(result.toString()).contains("dir2");
        }
    }

    @Nested
    @DisplayName("Delete File")
    class DeleteFileTests {

        @Test
        @DisplayName("Should delete existing file")
        void shouldDeleteExistingFile() throws IOException {
            // Given
            UUID noteId = UUID.randomUUID();
            MultipartFile file = new MockMultipartFile(
                    "test-file",
                    "to-delete.txt",
                    "text/plain",
                    "content".getBytes()
            );
            String storedPath = fileStorageService.storeFile(file, noteId);
            Path fullPath = fileStorageService.getFilePath(storedPath);
            assertThat(Files.exists(fullPath)).isTrue();

            // When
            fileStorageService.deleteFile(storedPath);

            // Then
            assertThat(Files.exists(fullPath)).isFalse();
        }

        @Test
        @DisplayName("Should not throw exception when file does not exist")
        void shouldNotThrowExceptionWhenFileDoesNotExist() throws IOException {
            // Given
            String nonExistentPath = "non-existent/file.txt";

            // When & Then
            fileStorageService.deleteFile(nonExistentPath);
            // No exception should be thrown
        }
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Should create upload directory if it does not exist")
        void shouldCreateUploadDirectoryIfNotExists() throws IOException {
            // Given
            Path newDir = tempDir.resolve("new-upload-dir");
            assertThat(Files.exists(newDir)).isFalse();

            // When
            new FileStorageService(newDir.toString());

            // Then
            assertThat(Files.exists(newDir)).isTrue();
            assertThat(Files.isDirectory(newDir)).isTrue();
        }
    }
}
