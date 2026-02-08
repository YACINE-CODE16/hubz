package com.hubz.application.service;

import com.hubz.application.dto.request.CreateChecklistItemRequest;
import com.hubz.application.dto.request.ReorderChecklistItemsRequest;
import com.hubz.application.dto.request.UpdateChecklistItemRequest;
import com.hubz.application.dto.response.ChecklistItemResponse;
import com.hubz.application.dto.response.ChecklistProgressResponse;
import com.hubz.application.port.out.ChecklistItemRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.exception.ChecklistItemNotFoundException;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.model.ChecklistItem;
import com.hubz.domain.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChecklistItemServiceTest {

    @Mock
    private ChecklistItemRepositoryPort checklistRepository;

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private ChecklistItemService checklistItemService;

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
    void shouldCreateChecklistItem() {
        // Given
        CreateChecklistItemRequest request = CreateChecklistItemRequest.builder()
                .content("Buy groceries")
                .build();

        ChecklistItem savedItem = ChecklistItem.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .content("Buy groceries")
                .completed(false)
                .position(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
        when(checklistRepository.getMaxPositionByTaskId(taskId)).thenReturn(-1);
        when(checklistRepository.save(any(ChecklistItem.class))).thenReturn(savedItem);

        // When
        ChecklistItemResponse response = checklistItemService.create(taskId, request, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("Buy groceries");
        assertThat(response.isCompleted()).isFalse();
        assertThat(response.getPosition()).isEqualTo(0);
        verify(checklistRepository).save(any(ChecklistItem.class));
    }

    @Test
    void shouldThrowExceptionWhenTaskNotFoundOnCreate() {
        // Given
        CreateChecklistItemRequest request = CreateChecklistItemRequest.builder()
                .content("Buy groceries")
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> checklistItemService.create(taskId, request, userId))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void shouldGetChecklistWithProgress() {
        // Given
        ChecklistItem item1 = ChecklistItem.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .content("Item 1")
                .completed(true)
                .position(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ChecklistItem item2 = ChecklistItem.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .content("Item 2")
                .completed(false)
                .position(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ChecklistItem item3 = ChecklistItem.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .content("Item 3")
                .completed(true)
                .position(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
        when(checklistRepository.findByTaskIdOrderByPosition(taskId))
                .thenReturn(Arrays.asList(item1, item2, item3));

        // When
        ChecklistProgressResponse response = checklistItemService.getChecklist(taskId, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTaskId()).isEqualTo(taskId);
        assertThat(response.getTotalItems()).isEqualTo(3);
        assertThat(response.getCompletedItems()).isEqualTo(2);
        assertThat(response.getCompletionPercentage()).isEqualTo(66.67);
        assertThat(response.getItems()).hasSize(3);
    }

    @Test
    void shouldUpdateChecklistItem() {
        // Given
        UUID itemId = UUID.randomUUID();
        UpdateChecklistItemRequest request = UpdateChecklistItemRequest.builder()
                .content("Updated content")
                .completed(true)
                .build();

        ChecklistItem existingItem = ChecklistItem.builder()
                .id(itemId)
                .taskId(taskId)
                .content("Original content")
                .completed(false)
                .position(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ChecklistItem updatedItem = ChecklistItem.builder()
                .id(itemId)
                .taskId(taskId)
                .content("Updated content")
                .completed(true)
                .position(0)
                .createdAt(existingItem.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(checklistRepository.findById(itemId)).thenReturn(Optional.of(existingItem));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
        when(checklistRepository.save(any(ChecklistItem.class))).thenReturn(updatedItem);

        // When
        ChecklistItemResponse response = checklistItemService.update(itemId, request, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("Updated content");
        assertThat(response.isCompleted()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenItemNotFoundOnUpdate() {
        // Given
        UUID itemId = UUID.randomUUID();
        UpdateChecklistItemRequest request = UpdateChecklistItemRequest.builder()
                .content("Updated content")
                .build();

        when(checklistRepository.findById(itemId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> checklistItemService.update(itemId, request, userId))
                .isInstanceOf(ChecklistItemNotFoundException.class);
    }

    @Test
    void shouldToggleChecklistItemCompleted() {
        // Given
        UUID itemId = UUID.randomUUID();
        ChecklistItem existingItem = ChecklistItem.builder()
                .id(itemId)
                .taskId(taskId)
                .content("Test item")
                .completed(false)
                .position(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ChecklistItem toggledItem = ChecklistItem.builder()
                .id(itemId)
                .taskId(taskId)
                .content("Test item")
                .completed(true)
                .position(0)
                .createdAt(existingItem.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(checklistRepository.findById(itemId)).thenReturn(Optional.of(existingItem));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
        when(checklistRepository.save(any(ChecklistItem.class))).thenReturn(toggledItem);

        // When
        ChecklistItemResponse response = checklistItemService.toggleCompleted(itemId, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isCompleted()).isTrue();
    }

    @Test
    void shouldReorderChecklistItems() {
        // Given
        UUID item1Id = UUID.randomUUID();
        UUID item2Id = UUID.randomUUID();
        UUID item3Id = UUID.randomUUID();

        ChecklistItem item1 = ChecklistItem.builder()
                .id(item1Id).taskId(taskId).content("Item 1").position(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        ChecklistItem item2 = ChecklistItem.builder()
                .id(item2Id).taskId(taskId).content("Item 2").position(1)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        ChecklistItem item3 = ChecklistItem.builder()
                .id(item3Id).taskId(taskId).content("Item 3").position(2)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        ReorderChecklistItemsRequest request = ReorderChecklistItemsRequest.builder()
                .itemIds(Arrays.asList(item3Id, item1Id, item2Id))
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
        when(checklistRepository.findByTaskId(taskId)).thenReturn(Arrays.asList(item1, item2, item3));
        when(checklistRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<ChecklistItemResponse> response = checklistItemService.reorder(taskId, request, userId);

        // Then
        assertThat(response).hasSize(3);
        verify(checklistRepository).saveAll(any());
    }

    @Test
    void shouldDeleteChecklistItem() {
        // Given
        UUID itemId = UUID.randomUUID();
        ChecklistItem existingItem = ChecklistItem.builder()
                .id(itemId)
                .taskId(taskId)
                .content("To delete")
                .position(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(checklistRepository.findById(itemId)).thenReturn(Optional.of(existingItem));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
        doNothing().when(checklistRepository).deleteById(itemId);

        // When
        checklistItemService.delete(itemId, userId);

        // Then
        verify(checklistRepository).deleteById(itemId);
    }

    @Test
    void shouldThrowExceptionWhenItemNotFoundOnDelete() {
        // Given
        UUID itemId = UUID.randomUUID();
        when(checklistRepository.findById(itemId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> checklistItemService.delete(itemId, userId))
                .isInstanceOf(ChecklistItemNotFoundException.class);
    }

    @Test
    void shouldCreateChecklistItemWithSpecifiedPosition() {
        // Given
        CreateChecklistItemRequest request = CreateChecklistItemRequest.builder()
                .content("New item at position 5")
                .position(5)
                .build();

        ChecklistItem savedItem = ChecklistItem.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .content("New item at position 5")
                .completed(false)
                .position(5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
        when(checklistRepository.save(any(ChecklistItem.class))).thenReturn(savedItem);

        // When
        ChecklistItemResponse response = checklistItemService.create(taskId, request, userId);

        // Then
        assertThat(response.getPosition()).isEqualTo(5);
    }
}
