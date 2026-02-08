package com.hubz.application.service;

import com.hubz.application.dto.request.CreateTagRequest;
import com.hubz.application.dto.request.UpdateTagRequest;
import com.hubz.application.dto.response.TagResponse;
import com.hubz.application.port.out.OrganizationDocumentRepositoryPort;
import com.hubz.application.port.out.TagRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.exception.TagNotFoundException;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.model.OrganizationDocument;
import com.hubz.domain.model.Tag;
import com.hubz.domain.model.Task;
import com.hubz.domain.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagService Unit Tests")
class TagServiceTest {

    @Mock
    private TagRepositoryPort tagRepository;

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private OrganizationDocumentRepositoryPort documentRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private TagService tagService;

    private UUID organizationId;
    private UUID userId;
    private UUID taskId;
    private UUID documentId;
    private Tag testTag;
    private Task testTask;
    private OrganizationDocument testDocument;
    private CreateTagRequest createRequest;
    private UpdateTagRequest updateRequest;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        documentId = UUID.randomUUID();

        testTag = Tag.builder()
                .id(UUID.randomUUID())
                .name("Bug")
                .color("#EF4444")
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .build();

        testTask = Task.builder()
                .id(taskId)
                .title("Test Task")
                .status(TaskStatus.TODO)
                .organizationId(organizationId)
                .creatorId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testDocument = OrganizationDocument.builder()
                .id(documentId)
                .organizationId(organizationId)
                .fileName("test-file.pdf")
                .originalFileName("test-file.pdf")
                .filePath("/uploads/test-file.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .uploadedBy(userId)
                .uploadedAt(LocalDateTime.now())
                .build();

        createRequest = CreateTagRequest.builder()
                .name("Bug")
                .color("#EF4444")
                .build();

        updateRequest = UpdateTagRequest.builder()
                .name("Feature")
                .color("#22C55E")
                .build();
    }

    @Nested
    @DisplayName("Create Tag Tests")
    class CreateTests {

        @Test
        @DisplayName("Should successfully create a tag")
        void shouldCreateTag() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(tagRepository.save(any(Tag.class))).thenReturn(testTag);

            // When
            TagResponse response = tagService.create(createRequest, organizationId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo(testTag.getName());
            assertThat(response.getColor()).isEqualTo(testTag.getColor());
            assertThat(response.getOrganizationId()).isEqualTo(organizationId);

            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(tagRepository).save(any(Tag.class));
        }

        @Test
        @DisplayName("Should set createdAt timestamp when creating tag")
        void shouldSetCreatedAtTimestamp() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
            when(tagRepository.save(tagCaptor.capture())).thenReturn(testTag);

            // When
            tagService.create(createRequest, organizationId, userId);

            // Then
            Tag savedTag = tagCaptor.getValue();
            assertThat(savedTag.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should check organization access before creating tag")
        void shouldCheckOrganizationAccess() {
            // Given
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> tagService.create(createRequest, organizationId, userId))
                    .isInstanceOf(RuntimeException.class);
            verify(tagRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Tags By Organization Tests")
    class GetByOrganizationTests {

        @Test
        @DisplayName("Should successfully get tags by organization")
        void shouldGetTagsByOrganization() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(tagRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testTag));

            // When
            List<TagResponse> tags = tagService.getByOrganization(organizationId, userId);

            // Then
            assertThat(tags).hasSize(1);
            assertThat(tags.get(0).getName()).isEqualTo(testTag.getName());
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(tagRepository).findByOrganizationId(organizationId);
        }

        @Test
        @DisplayName("Should return empty list when no tags exist")
        void shouldReturnEmptyListWhenNoTags() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(tagRepository.findByOrganizationId(organizationId)).thenReturn(List.of());

            // When
            List<TagResponse> tags = tagService.getByOrganization(organizationId, userId);

            // Then
            assertThat(tags).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Tag By Id Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should successfully get tag by id")
        void shouldGetTagById() {
            // Given
            when(tagRepository.findById(testTag.getId())).thenReturn(Optional.of(testTag));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            TagResponse response = tagService.getById(testTag.getId(), userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo(testTag.getName());
            verify(tagRepository).findById(testTag.getId());
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
        }

        @Test
        @DisplayName("Should throw exception when tag not found")
        void shouldThrowExceptionWhenTagNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(tagRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tagService.getById(nonExistentId, userId))
                    .isInstanceOf(TagNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Tag Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should successfully update tag")
        void shouldUpdateTag() {
            // Given
            when(tagRepository.findById(testTag.getId())).thenReturn(Optional.of(testTag));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(tagRepository.save(any(Tag.class))).thenReturn(testTag);

            // When
            TagResponse response = tagService.update(testTag.getId(), updateRequest, userId);

            // Then
            assertThat(response).isNotNull();
            verify(tagRepository).findById(testTag.getId());
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(tagRepository).save(any(Tag.class));
        }

        @Test
        @DisplayName("Should update tag name and color")
        void shouldUpdateTagNameAndColor() {
            // Given
            when(tagRepository.findById(testTag.getId())).thenReturn(Optional.of(testTag));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
            when(tagRepository.save(tagCaptor.capture())).thenReturn(testTag);

            // When
            tagService.update(testTag.getId(), updateRequest, userId);

            // Then
            Tag updatedTag = tagCaptor.getValue();
            assertThat(updatedTag.getName()).isEqualTo(updateRequest.getName());
            assertThat(updatedTag.getColor()).isEqualTo(updateRequest.getColor());
        }

        @Test
        @DisplayName("Should throw exception when tag not found")
        void shouldThrowExceptionWhenTagNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(tagRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tagService.update(nonExistentId, updateRequest, userId))
                    .isInstanceOf(TagNotFoundException.class);
            verify(tagRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Tag Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should successfully delete tag")
        void shouldDeleteTag() {
            // Given
            when(tagRepository.findById(testTag.getId())).thenReturn(Optional.of(testTag));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doNothing().when(tagRepository).deleteById(testTag.getId());

            // When
            tagService.delete(testTag.getId(), userId);

            // Then
            verify(tagRepository).findById(testTag.getId());
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(tagRepository).deleteById(testTag.getId());
        }

        @Test
        @DisplayName("Should throw exception when tag not found")
        void shouldThrowExceptionWhenTagNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(tagRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tagService.delete(nonExistentId, userId))
                    .isInstanceOf(TagNotFoundException.class);
            verify(tagRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Add Tag To Task Tests")
    class AddTagToTaskTests {

        @Test
        @DisplayName("Should successfully add tag to task")
        void shouldAddTagToTask() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            when(tagRepository.findById(testTag.getId())).thenReturn(Optional.of(testTag));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doNothing().when(tagRepository).addTagToTask(taskId, testTag.getId());

            // When
            tagService.addTagToTask(taskId, testTag.getId(), userId);

            // Then
            verify(taskRepository).findById(taskId);
            verify(tagRepository).findById(testTag.getId());
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(tagRepository).addTagToTask(taskId, testTag.getId());
        }

        @Test
        @DisplayName("Should throw exception when task not found")
        void shouldThrowExceptionWhenTaskNotFound() {
            // Given
            UUID nonExistentTaskId = UUID.randomUUID();
            when(taskRepository.findById(nonExistentTaskId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tagService.addTagToTask(nonExistentTaskId, testTag.getId(), userId))
                    .isInstanceOf(TaskNotFoundException.class);
            verify(tagRepository, never()).addTagToTask(any(), any());
        }

        @Test
        @DisplayName("Should throw exception when tag not found")
        void shouldThrowExceptionWhenTagNotFound() {
            // Given
            UUID nonExistentTagId = UUID.randomUUID();
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            when(tagRepository.findById(nonExistentTagId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tagService.addTagToTask(taskId, nonExistentTagId, userId))
                    .isInstanceOf(TagNotFoundException.class);
            verify(tagRepository, never()).addTagToTask(any(), any());
        }

        @Test
        @DisplayName("Should throw exception when tag belongs to different organization")
        void shouldThrowExceptionWhenTagBelongsToDifferentOrganization() {
            // Given
            Tag tagFromDifferentOrg = Tag.builder()
                    .id(UUID.randomUUID())
                    .name("Other Tag")
                    .color("#FF0000")
                    .organizationId(UUID.randomUUID()) // Different org
                    .createdAt(LocalDateTime.now())
                    .build();

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            when(tagRepository.findById(tagFromDifferentOrg.getId())).thenReturn(Optional.of(tagFromDifferentOrg));

            // When & Then
            assertThatThrownBy(() -> tagService.addTagToTask(taskId, tagFromDifferentOrg.getId(), userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("same organization");
            verify(tagRepository, never()).addTagToTask(any(), any());
        }
    }

    @Nested
    @DisplayName("Remove Tag From Task Tests")
    class RemoveTagFromTaskTests {

        @Test
        @DisplayName("Should successfully remove tag from task")
        void shouldRemoveTagFromTask() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doNothing().when(tagRepository).removeTagFromTask(taskId, testTag.getId());

            // When
            tagService.removeTagFromTask(taskId, testTag.getId(), userId);

            // Then
            verify(taskRepository).findById(taskId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(tagRepository).removeTagFromTask(taskId, testTag.getId());
        }

        @Test
        @DisplayName("Should throw exception when task not found")
        void shouldThrowExceptionWhenTaskNotFound() {
            // Given
            UUID nonExistentTaskId = UUID.randomUUID();
            when(taskRepository.findById(nonExistentTaskId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tagService.removeTagFromTask(nonExistentTaskId, testTag.getId(), userId))
                    .isInstanceOf(TaskNotFoundException.class);
            verify(tagRepository, never()).removeTagFromTask(any(), any());
        }
    }

    @Nested
    @DisplayName("Set Task Tags Tests")
    class SetTaskTagsTests {

        @Test
        @DisplayName("Should successfully set task tags")
        void shouldSetTaskTags() {
            // Given
            Set<UUID> tagIds = Set.of(testTag.getId());
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(tagRepository.findByIds(tagIds)).thenReturn(List.of(testTag));
            doNothing().when(tagRepository).removeAllTagsFromTask(taskId);
            doNothing().when(tagRepository).addTagToTask(taskId, testTag.getId());

            // When
            tagService.setTaskTags(taskId, tagIds, userId);

            // Then
            verify(taskRepository).findById(taskId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(tagRepository).removeAllTagsFromTask(taskId);
            verify(tagRepository).addTagToTask(taskId, testTag.getId());
        }

        @Test
        @DisplayName("Should remove all tags when empty set is provided")
        void shouldRemoveAllTagsWhenEmptySetProvided() {
            // Given
            Set<UUID> emptyTagIds = Set.of();
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doNothing().when(tagRepository).removeAllTagsFromTask(taskId);

            // When
            tagService.setTaskTags(taskId, emptyTagIds, userId);

            // Then
            verify(tagRepository).removeAllTagsFromTask(taskId);
            verify(tagRepository, never()).addTagToTask(any(), any());
        }
    }

    @Nested
    @DisplayName("Get Tags By Task Tests")
    class GetTagsByTaskTests {

        @Test
        @DisplayName("Should successfully get tags by task")
        void shouldGetTagsByTask() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(tagRepository.findTagsByTaskId(taskId)).thenReturn(List.of(testTag));

            // When
            List<TagResponse> tags = tagService.getTagsByTask(taskId, userId);

            // Then
            assertThat(tags).hasSize(1);
            assertThat(tags.get(0).getName()).isEqualTo(testTag.getName());
            verify(taskRepository).findById(taskId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(tagRepository).findTagsByTaskId(taskId);
        }

        @Test
        @DisplayName("Should throw exception when task not found")
        void shouldThrowExceptionWhenTaskNotFound() {
            // Given
            UUID nonExistentTaskId = UUID.randomUUID();
            when(taskRepository.findById(nonExistentTaskId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tagService.getTagsByTask(nonExistentTaskId, userId))
                    .isInstanceOf(TaskNotFoundException.class);
            verify(tagRepository, never()).findTagsByTaskId(any());
        }
    }

    // Document-Tag Tests

    @Nested
    @DisplayName("Add Tag To Document Tests")
    class AddTagToDocumentTests {

        @Test
        @DisplayName("Should successfully add tag to document")
        void shouldAddTagToDocument() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            when(tagRepository.findById(testTag.getId())).thenReturn(Optional.of(testTag));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doNothing().when(tagRepository).addTagToDocument(documentId, testTag.getId());

            // When
            tagService.addTagToDocument(documentId, testTag.getId(), userId);

            // Then
            verify(documentRepository).findById(documentId);
            verify(tagRepository).findById(testTag.getId());
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(tagRepository).addTagToDocument(documentId, testTag.getId());
        }

        @Test
        @DisplayName("Should throw exception when document not found")
        void shouldThrowExceptionWhenDocumentNotFound() {
            // Given
            UUID nonExistentDocId = UUID.randomUUID();
            when(documentRepository.findById(nonExistentDocId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tagService.addTagToDocument(nonExistentDocId, testTag.getId(), userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Document not found");
            verify(tagRepository, never()).addTagToDocument(any(), any());
        }

        @Test
        @DisplayName("Should throw exception when tag not found")
        void shouldThrowExceptionWhenTagNotFound() {
            // Given
            UUID nonExistentTagId = UUID.randomUUID();
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            when(tagRepository.findById(nonExistentTagId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tagService.addTagToDocument(documentId, nonExistentTagId, userId))
                    .isInstanceOf(TagNotFoundException.class);
            verify(tagRepository, never()).addTagToDocument(any(), any());
        }

        @Test
        @DisplayName("Should throw exception when tag belongs to different organization")
        void shouldThrowExceptionWhenTagBelongsToDifferentOrganization() {
            // Given
            Tag tagFromDifferentOrg = Tag.builder()
                    .id(UUID.randomUUID())
                    .name("Other Tag")
                    .color("#FF0000")
                    .organizationId(UUID.randomUUID()) // Different org
                    .createdAt(LocalDateTime.now())
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            when(tagRepository.findById(tagFromDifferentOrg.getId())).thenReturn(Optional.of(tagFromDifferentOrg));

            // When & Then
            assertThatThrownBy(() -> tagService.addTagToDocument(documentId, tagFromDifferentOrg.getId(), userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("same organization");
            verify(tagRepository, never()).addTagToDocument(any(), any());
        }
    }

    @Nested
    @DisplayName("Remove Tag From Document Tests")
    class RemoveTagFromDocumentTests {

        @Test
        @DisplayName("Should successfully remove tag from document")
        void shouldRemoveTagFromDocument() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doNothing().when(tagRepository).removeTagFromDocument(documentId, testTag.getId());

            // When
            tagService.removeTagFromDocument(documentId, testTag.getId(), userId);

            // Then
            verify(documentRepository).findById(documentId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(tagRepository).removeTagFromDocument(documentId, testTag.getId());
        }

        @Test
        @DisplayName("Should throw exception when document not found")
        void shouldThrowExceptionWhenDocumentNotFound() {
            // Given
            UUID nonExistentDocId = UUID.randomUUID();
            when(documentRepository.findById(nonExistentDocId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tagService.removeTagFromDocument(nonExistentDocId, testTag.getId(), userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Document not found");
            verify(tagRepository, never()).removeTagFromDocument(any(), any());
        }
    }

    @Nested
    @DisplayName("Set Document Tags Tests")
    class SetDocumentTagsTests {

        @Test
        @DisplayName("Should successfully set document tags")
        void shouldSetDocumentTags() {
            // Given
            Set<UUID> tagIds = Set.of(testTag.getId());
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(tagRepository.findByIds(tagIds)).thenReturn(List.of(testTag));
            doNothing().when(tagRepository).removeAllTagsFromDocument(documentId);
            doNothing().when(tagRepository).addTagToDocument(documentId, testTag.getId());

            // When
            tagService.setDocumentTags(documentId, tagIds, userId);

            // Then
            verify(documentRepository).findById(documentId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(tagRepository).removeAllTagsFromDocument(documentId);
            verify(tagRepository).addTagToDocument(documentId, testTag.getId());
        }

        @Test
        @DisplayName("Should remove all tags when empty set is provided")
        void shouldRemoveAllTagsWhenEmptySetProvided() {
            // Given
            Set<UUID> emptyTagIds = Set.of();
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doNothing().when(tagRepository).removeAllTagsFromDocument(documentId);

            // When
            tagService.setDocumentTags(documentId, emptyTagIds, userId);

            // Then
            verify(tagRepository).removeAllTagsFromDocument(documentId);
            verify(tagRepository, never()).addTagToDocument(any(), any());
        }

        @Test
        @DisplayName("Should throw exception when tag belongs to different organization")
        void shouldThrowExceptionWhenTagBelongsToDifferentOrganization() {
            // Given
            Tag tagFromDifferentOrg = Tag.builder()
                    .id(UUID.randomUUID())
                    .name("Other Tag")
                    .color("#FF0000")
                    .organizationId(UUID.randomUUID()) // Different org
                    .createdAt(LocalDateTime.now())
                    .build();
            Set<UUID> tagIds = Set.of(tagFromDifferentOrg.getId());

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(tagRepository.findByIds(tagIds)).thenReturn(List.of(tagFromDifferentOrg));

            // When & Then
            assertThatThrownBy(() -> tagService.setDocumentTags(documentId, tagIds, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("same organization");
            verify(tagRepository, never()).addTagToDocument(any(), any());
        }
    }

    @Nested
    @DisplayName("Get Tags By Document Tests")
    class GetTagsByDocumentTests {

        @Test
        @DisplayName("Should successfully get tags by document")
        void shouldGetTagsByDocument() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(tagRepository.findTagsByDocumentId(documentId)).thenReturn(List.of(testTag));

            // When
            List<TagResponse> tags = tagService.getTagsByDocument(documentId, userId);

            // Then
            assertThat(tags).hasSize(1);
            assertThat(tags.get(0).getName()).isEqualTo(testTag.getName());
            verify(documentRepository).findById(documentId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(tagRepository).findTagsByDocumentId(documentId);
        }

        @Test
        @DisplayName("Should throw exception when document not found")
        void shouldThrowExceptionWhenDocumentNotFound() {
            // Given
            UUID nonExistentDocId = UUID.randomUUID();
            when(documentRepository.findById(nonExistentDocId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tagService.getTagsByDocument(nonExistentDocId, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Document not found");
            verify(tagRepository, never()).findTagsByDocumentId(any());
        }

        @Test
        @DisplayName("Should return empty list when no tags exist")
        void shouldReturnEmptyListWhenNoTags() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(tagRepository.findTagsByDocumentId(documentId)).thenReturn(List.of());

            // When
            List<TagResponse> tags = tagService.getTagsByDocument(documentId, userId);

            // Then
            assertThat(tags).isEmpty();
        }
    }
}
