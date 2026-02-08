package com.hubz.application.service;

import com.hubz.application.dto.request.CreateNoteFolderRequest;
import com.hubz.application.dto.request.UpdateNoteFolderRequest;
import com.hubz.application.dto.response.NoteFolderResponse;
import com.hubz.application.port.out.NoteFolderRepositoryPort;
import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.domain.exception.NoteFolderNotFoundException;
import com.hubz.domain.model.Note;
import com.hubz.domain.model.NoteFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteFolderServiceTest {

    @Mock
    private NoteFolderRepositoryPort folderRepository;

    @Mock
    private NoteRepositoryPort noteRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private NoteFolderService noteFolderService;

    private UUID organizationId;
    private UUID userId;
    private UUID folderId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        folderId = UUID.randomUUID();
    }

    @Test
    void shouldCreateFolder() {
        // Given
        CreateNoteFolderRequest request = CreateNoteFolderRequest.builder()
                .name("Test Folder")
                .build();

        NoteFolder savedFolder = NoteFolder.builder()
                .id(folderId)
                .name("Test Folder")
                .organizationId(organizationId)
                .createdById(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(folderRepository.save(any(NoteFolder.class))).thenReturn(savedFolder);

        // When
        NoteFolderResponse response = noteFolderService.create(request, organizationId, userId);

        // Then
        assertThat(response.getName()).isEqualTo("Test Folder");
        assertThat(response.getOrganizationId()).isEqualTo(organizationId);
        verify(authorizationService).checkOrganizationAccess(organizationId, userId);
        verify(folderRepository).save(any(NoteFolder.class));
    }

    @Test
    void shouldCreateSubfolder() {
        // Given
        UUID parentFolderId = UUID.randomUUID();
        CreateNoteFolderRequest request = CreateNoteFolderRequest.builder()
                .name("Subfolder")
                .parentFolderId(parentFolderId)
                .build();

        NoteFolder parentFolder = NoteFolder.builder()
                .id(parentFolderId)
                .name("Parent")
                .organizationId(organizationId)
                .build();

        NoteFolder savedFolder = NoteFolder.builder()
                .id(folderId)
                .name("Subfolder")
                .parentFolderId(parentFolderId)
                .organizationId(organizationId)
                .createdById(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(folderRepository.findById(parentFolderId)).thenReturn(Optional.of(parentFolder));
        when(folderRepository.save(any(NoteFolder.class))).thenReturn(savedFolder);

        // When
        NoteFolderResponse response = noteFolderService.create(request, organizationId, userId);

        // Then
        assertThat(response.getName()).isEqualTo("Subfolder");
        assertThat(response.getParentFolderId()).isEqualTo(parentFolderId);
    }

    @Test
    void shouldThrowExceptionWhenParentFolderNotFound() {
        // Given
        UUID nonExistentParentId = UUID.randomUUID();
        CreateNoteFolderRequest request = CreateNoteFolderRequest.builder()
                .name("Subfolder")
                .parentFolderId(nonExistentParentId)
                .build();

        when(folderRepository.findById(nonExistentParentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteFolderService.create(request, organizationId, userId))
                .isInstanceOf(NoteFolderNotFoundException.class);
    }

    @Test
    void shouldThrowExceptionWhenParentFolderInDifferentOrganization() {
        // Given
        UUID parentFolderId = UUID.randomUUID();
        UUID otherOrgId = UUID.randomUUID();
        CreateNoteFolderRequest request = CreateNoteFolderRequest.builder()
                .name("Subfolder")
                .parentFolderId(parentFolderId)
                .build();

        NoteFolder parentFolder = NoteFolder.builder()
                .id(parentFolderId)
                .name("Parent")
                .organizationId(otherOrgId)
                .build();

        when(folderRepository.findById(parentFolderId)).thenReturn(Optional.of(parentFolder));

        // When & Then
        assertThatThrownBy(() -> noteFolderService.create(request, organizationId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parent folder does not belong to this organization");
    }

    @Test
    void shouldGetFolderById() {
        // Given
        NoteFolder folder = NoteFolder.builder()
                .id(folderId)
                .name("Test Folder")
                .organizationId(organizationId)
                .createdById(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
        when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

        // When
        NoteFolderResponse response = noteFolderService.getById(folderId, userId);

        // Then
        assertThat(response.getId()).isEqualTo(folderId);
        assertThat(response.getName()).isEqualTo("Test Folder");
        verify(authorizationService).checkOrganizationAccess(organizationId, userId);
    }

    @Test
    void shouldThrowExceptionWhenFolderNotFound() {
        // Given
        when(folderRepository.findById(folderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteFolderService.getById(folderId, userId))
                .isInstanceOf(NoteFolderNotFoundException.class);
    }

    @Test
    void shouldUpdateFolder() {
        // Given
        UpdateNoteFolderRequest request = UpdateNoteFolderRequest.builder()
                .name("Updated Folder")
                .build();

        NoteFolder existingFolder = NoteFolder.builder()
                .id(folderId)
                .name("Test Folder")
                .organizationId(organizationId)
                .createdById(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        NoteFolder updatedFolder = NoteFolder.builder()
                .id(folderId)
                .name("Updated Folder")
                .organizationId(organizationId)
                .createdById(userId)
                .createdAt(existingFolder.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(folderRepository.findById(folderId)).thenReturn(Optional.of(existingFolder));
        when(folderRepository.save(any(NoteFolder.class))).thenReturn(updatedFolder);
        when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

        // When
        NoteFolderResponse response = noteFolderService.update(folderId, request, userId);

        // Then
        assertThat(response.getName()).isEqualTo("Updated Folder");
        verify(folderRepository).save(any(NoteFolder.class));
    }

    @Test
    void shouldNotAllowFolderToBeItsOwnParent() {
        // Given
        UpdateNoteFolderRequest request = UpdateNoteFolderRequest.builder()
                .parentFolderId(folderId) // Setting itself as parent
                .build();

        NoteFolder existingFolder = NoteFolder.builder()
                .id(folderId)
                .name("Test Folder")
                .organizationId(organizationId)
                .build();

        when(folderRepository.findById(folderId)).thenReturn(Optional.of(existingFolder));

        // When & Then
        assertThatThrownBy(() -> noteFolderService.update(folderId, request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be its own parent");
    }

    @Test
    void shouldDeleteEmptyFolder() {
        // Given
        NoteFolder folder = NoteFolder.builder()
                .id(folderId)
                .name("Test Folder")
                .organizationId(organizationId)
                .build();

        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
        when(folderRepository.countChildFolders(folderId)).thenReturn(0L);
        when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

        // When
        noteFolderService.delete(folderId, userId);

        // Then
        verify(folderRepository).deleteById(folderId);
    }

    @Test
    void shouldNotDeleteFolderWithSubfolders() {
        // Given
        NoteFolder folder = NoteFolder.builder()
                .id(folderId)
                .name("Test Folder")
                .organizationId(organizationId)
                .build();

        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
        when(folderRepository.countChildFolders(folderId)).thenReturn(2L);

        // When & Then
        assertThatThrownBy(() -> noteFolderService.delete(folderId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete folder with subfolders");
    }

    @Test
    void shouldNotDeleteFolderWithNotes() {
        // Given
        NoteFolder folder = NoteFolder.builder()
                .id(folderId)
                .name("Test Folder")
                .organizationId(organizationId)
                .build();

        Note noteInFolder = Note.builder()
                .id(UUID.randomUUID())
                .folderId(folderId)
                .organizationId(organizationId)
                .build();

        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
        when(folderRepository.countChildFolders(folderId)).thenReturn(0L);
        when(noteRepository.findByOrganizationId(organizationId)).thenReturn(List.of(noteInFolder));

        // When & Then
        assertThatThrownBy(() -> noteFolderService.delete(folderId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete folder with notes");
    }

    @Test
    void shouldGetFoldersByOrganizationAsTree() {
        // Given
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        NoteFolder parentFolder = NoteFolder.builder()
                .id(parentId)
                .name("Parent")
                .parentFolderId(null)
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        NoteFolder childFolder = NoteFolder.builder()
                .id(childId)
                .name("Child")
                .parentFolderId(parentId)
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(folderRepository.findByOrganizationId(organizationId))
                .thenReturn(List.of(parentFolder, childFolder));
        when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

        // When
        List<NoteFolderResponse> response = noteFolderService.getByOrganization(organizationId, userId);

        // Then
        assertThat(response).hasSize(1); // Only root folder
        assertThat(response.get(0).getName()).isEqualTo("Parent");
        assertThat(response.get(0).getChildren()).hasSize(1);
        assertThat(response.get(0).getChildren().get(0).getName()).isEqualTo("Child");
    }

    @Test
    void shouldGetFlatListOfFolders() {
        // Given
        NoteFolder folder1 = NoteFolder.builder()
                .id(UUID.randomUUID())
                .name("Folder 1")
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        NoteFolder folder2 = NoteFolder.builder()
                .id(UUID.randomUUID())
                .name("Folder 2")
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(folderRepository.findByOrganizationId(organizationId))
                .thenReturn(List.of(folder1, folder2));
        when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

        // When
        List<NoteFolderResponse> response = noteFolderService.getFlatList(organizationId, userId);

        // Then
        assertThat(response).hasSize(2);
        assertThat(response).extracting(NoteFolderResponse::getName)
                .containsExactlyInAnyOrder("Folder 1", "Folder 2");
    }
}
