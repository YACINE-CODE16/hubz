package com.hubz.application.service;

import com.hubz.application.dto.request.CreateTaskRequest;
import com.hubz.application.dto.request.UpdateTaskRequest;
import com.hubz.application.dto.request.UpdateTaskStatusRequest;
import com.hubz.application.dto.response.TaskResponse;
import com.hubz.application.port.out.TagRepositoryPort;
import com.hubz.application.port.out.TaskHistoryRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.model.Task;
import com.hubz.domain.model.TaskHistory;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
class TaskServiceTest {

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private TagRepositoryPort tagRepository;

    @Mock
    private TaskHistoryRepositoryPort taskHistoryRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private TaskService taskService;

    private UUID organizationId;
    private UUID creatorId;
    private UUID assigneeId;
    private Task testTask;
    private CreateTaskRequest createRequest;
    private UpdateTaskRequest updateRequest;
    private UpdateTaskStatusRequest statusRequest;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();

        testTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Test Task")
                .description("Test description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .organizationId(organizationId)
                .assigneeId(assigneeId)
                .creatorId(creatorId)
                .goalId(null)
                .dueDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = new CreateTaskRequest(
                "Test Task",
                "Test description",
                TaskPriority.MEDIUM,
                null,
                assigneeId,
                LocalDateTime.now().plusDays(7)
        );

        updateRequest = new UpdateTaskRequest(
                "Updated Task",
                "Updated description",
                TaskPriority.HIGH,
                null,
                assigneeId,
                LocalDateTime.now().plusDays(14)
        );

        statusRequest = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);
    }

    @Nested
    @DisplayName("Create Task Tests")
    class CreateTests {

        @Test
        @DisplayName("Should successfully create task")
        void shouldCreateTask() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, creatorId);
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            when(tagRepository.findTagsByTaskId(any())).thenReturn(Collections.emptyList());

            // When
            TaskResponse response = taskService.create(createRequest, organizationId, creatorId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo(testTask.getTitle());
            assertThat(response.getDescription()).isEqualTo(testTask.getDescription());
            assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(response.getPriority()).isEqualTo(testTask.getPriority());
            assertThat(response.getOrganizationId()).isEqualTo(organizationId);
            assertThat(response.getCreatorId()).isEqualTo(creatorId);

            verify(authorizationService).checkOrganizationAccess(organizationId, creatorId);
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should set task status to TODO by default")
        void shouldSetStatusToTodoByDefault() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, creatorId);
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            when(taskRepository.save(taskCaptor.capture())).thenReturn(testTask);

            // When
            taskService.create(createRequest, organizationId, creatorId);

            // Then
            Task savedTask = taskCaptor.getValue();
            assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        @DisplayName("Should set createdAt and updatedAt timestamps")
        void shouldSetTimestamps() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, creatorId);
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            when(taskRepository.save(taskCaptor.capture())).thenReturn(testTask);

            // When
            taskService.create(createRequest, organizationId, creatorId);

            // Then
            Task savedTask = taskCaptor.getValue();
            assertThat(savedTask.getCreatedAt()).isNotNull();
            assertThat(savedTask.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should check organization access before creating task")
        void shouldCheckOrganizationAccess() {
            // Given
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, creatorId);

            // When & Then
            assertThatThrownBy(() -> taskService.create(createRequest, organizationId, creatorId))
                    .isInstanceOf(RuntimeException.class);
            verify(taskRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Tasks By Organization Tests")
    class GetByOrganizationTests {

        @Test
        @DisplayName("Should successfully get tasks by organization")
        void shouldGetTasksByOrganization() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, creatorId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testTask));
            when(tagRepository.findTagsByTaskId(any())).thenReturn(Collections.emptyList());

            // When
            List<TaskResponse> tasks = taskService.getByOrganization(organizationId, creatorId);

            // Then
            assertThat(tasks).hasSize(1);
            assertThat(tasks.get(0).getTitle()).isEqualTo(testTask.getTitle());
            verify(authorizationService).checkOrganizationAccess(organizationId, creatorId);
            verify(taskRepository).findByOrganizationId(organizationId);
        }

        @Test
        @DisplayName("Should return empty list when no tasks exist")
        void shouldReturnEmptyListWhenNoTasks() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, creatorId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(List.of());

            // When
            List<TaskResponse> tasks = taskService.getByOrganization(organizationId, creatorId);

            // Then
            assertThat(tasks).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Tasks By User Tests")
    class GetByUserTests {

        @Test
        @DisplayName("Should successfully get tasks by assignee")
        void shouldGetTasksByAssignee() {
            // Given
            when(taskRepository.findByAssigneeId(assigneeId)).thenReturn(List.of(testTask));
            when(tagRepository.findTagsByTaskId(any())).thenReturn(Collections.emptyList());

            // When
            List<TaskResponse> tasks = taskService.getByUser(assigneeId);

            // Then
            assertThat(tasks).hasSize(1);
            assertThat(tasks.get(0).getAssigneeId()).isEqualTo(assigneeId);
            verify(taskRepository).findByAssigneeId(assigneeId);
        }

        @Test
        @DisplayName("Should return empty list when user has no tasks")
        void shouldReturnEmptyListWhenNoTasks() {
            // Given
            when(taskRepository.findByAssigneeId(assigneeId)).thenReturn(List.of());

            // When
            List<TaskResponse> tasks = taskService.getByUser(assigneeId);

            // Then
            assertThat(tasks).isEmpty();
        }
    }

    @Nested
    @DisplayName("Update Task Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should successfully update task")
        void shouldUpdateTask() {
            // Given
            when(taskRepository.findById(testTask.getId())).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, creatorId);
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            when(tagRepository.findTagsByTaskId(any())).thenReturn(Collections.emptyList());

            // When
            TaskResponse response = taskService.update(testTask.getId(), updateRequest, creatorId);

            // Then
            assertThat(response).isNotNull();
            verify(taskRepository).findById(testTask.getId());
            verify(authorizationService).checkOrganizationAccess(organizationId, creatorId);
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should update updatedAt timestamp")
        void shouldUpdateTimestamp() {
            // Given
            when(taskRepository.findById(testTask.getId())).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, creatorId);
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            when(taskRepository.save(taskCaptor.capture())).thenReturn(testTask);
            when(tagRepository.findTagsByTaskId(any())).thenReturn(Collections.emptyList());

            // When
            taskService.update(testTask.getId(), updateRequest, creatorId);

            // Then
            Task updatedTask = taskCaptor.getValue();
            assertThat(updatedTask.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when task not found")
        void shouldThrowExceptionWhenTaskNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(taskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> taskService.update(nonExistentId, updateRequest, creatorId))
                    .isInstanceOf(TaskNotFoundException.class);
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should check organization access before updating")
        void shouldCheckOrganizationAccessBeforeUpdate() {
            // Given
            when(taskRepository.findById(testTask.getId())).thenReturn(Optional.of(testTask));
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, creatorId);

            // When & Then
            assertThatThrownBy(() -> taskService.update(testTask.getId(), updateRequest, creatorId))
                    .isInstanceOf(RuntimeException.class);
            verify(taskRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Update Task Status Tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should successfully update task status")
        void shouldUpdateTaskStatus() {
            // Given
            when(taskRepository.findById(testTask.getId())).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, creatorId);
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            when(tagRepository.findTagsByTaskId(any())).thenReturn(Collections.emptyList());

            // When
            TaskResponse response = taskService.updateStatus(testTask.getId(), statusRequest, creatorId);

            // Then
            assertThat(response).isNotNull();
            verify(taskRepository).findById(testTask.getId());
            verify(authorizationService).checkOrganizationAccess(organizationId, creatorId);
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should update updatedAt when changing status")
        void shouldUpdateTimestampWhenChangingStatus() {
            // Given
            when(taskRepository.findById(testTask.getId())).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, creatorId);
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            when(taskRepository.save(taskCaptor.capture())).thenReturn(testTask);
            when(tagRepository.findTagsByTaskId(any())).thenReturn(Collections.emptyList());

            // When
            taskService.updateStatus(testTask.getId(), statusRequest, creatorId);

            // Then
            Task updatedTask = taskCaptor.getValue();
            assertThat(updatedTask.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when task not found")
        void shouldThrowExceptionWhenTaskNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(taskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> taskService.updateStatus(nonExistentId, statusRequest, creatorId))
                    .isInstanceOf(TaskNotFoundException.class);
            verify(taskRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Task Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should successfully delete task")
        void shouldDeleteTask() {
            // Given
            when(taskRepository.findById(testTask.getId())).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, creatorId);
            doNothing().when(taskRepository).deleteById(testTask.getId());

            // When
            taskService.delete(testTask.getId(), creatorId);

            // Then
            verify(taskRepository).findById(testTask.getId());
            verify(authorizationService).checkOrganizationAccess(organizationId, creatorId);
            verify(taskRepository).deleteById(testTask.getId());
        }

        @Test
        @DisplayName("Should throw exception when task not found")
        void shouldThrowExceptionWhenTaskNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(taskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> taskService.delete(nonExistentId, creatorId))
                    .isInstanceOf(TaskNotFoundException.class);
            verify(taskRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should check organization access before deleting")
        void shouldCheckOrganizationAccessBeforeDelete() {
            // Given
            when(taskRepository.findById(testTask.getId())).thenReturn(Optional.of(testTask));
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, creatorId);

            // When & Then
            assertThatThrownBy(() -> taskService.delete(testTask.getId(), creatorId))
                    .isInstanceOf(RuntimeException.class);
            verify(taskRepository, never()).deleteById(any());
        }
    }
}
