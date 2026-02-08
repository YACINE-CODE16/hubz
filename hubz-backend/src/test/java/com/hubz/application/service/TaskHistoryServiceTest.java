package com.hubz.application.service;

import com.hubz.application.dto.response.TaskHistoryResponse;
import com.hubz.application.port.out.TaskHistoryRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.TaskHistoryField;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.model.Task;
import com.hubz.domain.model.TaskHistory;
import com.hubz.domain.model.User;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskHistoryService Unit Tests")
class TaskHistoryServiceTest {

    @Mock
    private TaskHistoryRepositoryPort taskHistoryRepository;

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private TaskHistoryService taskHistoryService;

    private UUID taskId;
    private UUID userId;
    private UUID organizationId;
    private Task testTask;
    private User testUser;
    private TaskHistory testHistory;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        testTask = Task.builder()
                .id(taskId)
                .title("Test Task")
                .description("Test description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .organizationId(organizationId)
                .assigneeId(userId)
                .creatorId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .profilePhotoUrl("https://example.com/photo.jpg")
                .build();

        testHistory = TaskHistory.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .userId(userId)
                .fieldChanged(TaskHistoryField.STATUS)
                .oldValue("TODO")
                .newValue("IN_PROGRESS")
                .changedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Get Task History Tests")
    class GetTaskHistoryTests {

        @Test
        @DisplayName("Should successfully get task history")
        void shouldGetTaskHistory() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskHistoryRepository.findByTaskId(taskId)).thenReturn(List.of(testHistory));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            List<TaskHistoryResponse> history = taskHistoryService.getTaskHistory(taskId, userId);

            // Then
            assertThat(history).hasSize(1);
            assertThat(history.get(0).getTaskId()).isEqualTo(taskId);
            assertThat(history.get(0).getFieldChanged()).isEqualTo(TaskHistoryField.STATUS);
            assertThat(history.get(0).getOldValue()).isEqualTo("TODO");
            assertThat(history.get(0).getNewValue()).isEqualTo("IN_PROGRESS");
            assertThat(history.get(0).getUserName()).isEqualTo("John Doe");
            assertThat(history.get(0).getUserPhotoUrl()).isEqualTo("https://example.com/photo.jpg");

            verify(taskRepository).findById(taskId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(taskHistoryRepository).findByTaskId(taskId);
        }

        @Test
        @DisplayName("Should return empty list when no history exists")
        void shouldReturnEmptyListWhenNoHistory() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskHistoryRepository.findByTaskId(taskId)).thenReturn(List.of());

            // When
            List<TaskHistoryResponse> history = taskHistoryService.getTaskHistory(taskId, userId);

            // Then
            assertThat(history).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when task not found")
        void shouldThrowExceptionWhenTaskNotFound() {
            // Given
            UUID nonExistentTaskId = UUID.randomUUID();
            when(taskRepository.findById(nonExistentTaskId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> taskHistoryService.getTaskHistory(nonExistentTaskId, userId))
                    .isInstanceOf(TaskNotFoundException.class);

            verify(taskHistoryRepository, never()).findByTaskId(any());
        }

        @Test
        @DisplayName("Should check organization access")
        void shouldCheckOrganizationAccess() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> taskHistoryService.getTaskHistory(taskId, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("No access");

            verify(taskHistoryRepository, never()).findByTaskId(any());
        }

        @Test
        @DisplayName("Should handle unknown user in history")
        void shouldHandleUnknownUserInHistory() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskHistoryRepository.findByTaskId(taskId)).thenReturn(List.of(testHistory));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When
            List<TaskHistoryResponse> history = taskHistoryService.getTaskHistory(taskId, userId);

            // Then
            assertThat(history).hasSize(1);
            assertThat(history.get(0).getUserName()).isEqualTo("Unknown User");
            assertThat(history.get(0).getUserPhotoUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("Get Task History By Field Tests")
    class GetTaskHistoryByFieldTests {

        @Test
        @DisplayName("Should successfully get task history filtered by field")
        void shouldGetTaskHistoryByField() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskHistoryRepository.findByTaskIdAndFieldChanged(taskId, TaskHistoryField.STATUS))
                    .thenReturn(List.of(testHistory));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            List<TaskHistoryResponse> history = taskHistoryService.getTaskHistoryByField(
                    taskId, TaskHistoryField.STATUS, userId);

            // Then
            assertThat(history).hasSize(1);
            assertThat(history.get(0).getFieldChanged()).isEqualTo(TaskHistoryField.STATUS);

            verify(taskHistoryRepository).findByTaskIdAndFieldChanged(taskId, TaskHistoryField.STATUS);
        }

        @Test
        @DisplayName("Should return empty list when no matching history")
        void shouldReturnEmptyListWhenNoMatchingHistory() {
            // Given
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskHistoryRepository.findByTaskIdAndFieldChanged(taskId, TaskHistoryField.TITLE))
                    .thenReturn(List.of());

            // When
            List<TaskHistoryResponse> history = taskHistoryService.getTaskHistoryByField(
                    taskId, TaskHistoryField.TITLE, userId);

            // Then
            assertThat(history).isEmpty();
        }
    }

    @Nested
    @DisplayName("Record Changes Tests")
    class RecordChangesTests {

        @Test
        @DisplayName("Should record title change")
        void shouldRecordTitleChange() {
            // Given
            Task oldTask = createTestTask();
            Task newTask = createTestTask();
            newTask.setTitle("New Title");

            // When
            taskHistoryService.recordChanges(oldTask, newTask, userId);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<TaskHistory>> captor = ArgumentCaptor.forClass(List.class);
            verify(taskHistoryRepository).saveAll(captor.capture());

            List<TaskHistory> savedHistories = captor.getValue();
            assertThat(savedHistories).hasSize(1);
            assertThat(savedHistories.get(0).getFieldChanged()).isEqualTo(TaskHistoryField.TITLE);
            assertThat(savedHistories.get(0).getOldValue()).isEqualTo("Test Task");
            assertThat(savedHistories.get(0).getNewValue()).isEqualTo("New Title");
        }

        @Test
        @DisplayName("Should record status change")
        void shouldRecordStatusChange() {
            // Given
            Task oldTask = createTestTask();
            Task newTask = createTestTask();
            newTask.setStatus(TaskStatus.IN_PROGRESS);

            // When
            taskHistoryService.recordChanges(oldTask, newTask, userId);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<TaskHistory>> captor = ArgumentCaptor.forClass(List.class);
            verify(taskHistoryRepository).saveAll(captor.capture());

            List<TaskHistory> savedHistories = captor.getValue();
            assertThat(savedHistories).hasSize(1);
            assertThat(savedHistories.get(0).getFieldChanged()).isEqualTo(TaskHistoryField.STATUS);
            assertThat(savedHistories.get(0).getOldValue()).isEqualTo("TODO");
            assertThat(savedHistories.get(0).getNewValue()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("Should record priority change")
        void shouldRecordPriorityChange() {
            // Given
            Task oldTask = createTestTask();
            Task newTask = createTestTask();
            newTask.setPriority(TaskPriority.HIGH);

            // When
            taskHistoryService.recordChanges(oldTask, newTask, userId);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<TaskHistory>> captor = ArgumentCaptor.forClass(List.class);
            verify(taskHistoryRepository).saveAll(captor.capture());

            List<TaskHistory> savedHistories = captor.getValue();
            assertThat(savedHistories).hasSize(1);
            assertThat(savedHistories.get(0).getFieldChanged()).isEqualTo(TaskHistoryField.PRIORITY);
            assertThat(savedHistories.get(0).getOldValue()).isEqualTo("MEDIUM");
            assertThat(savedHistories.get(0).getNewValue()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("Should record assignee change")
        void shouldRecordAssigneeChange() {
            // Given
            UUID newAssigneeId = UUID.randomUUID();
            Task oldTask = createTestTask();
            Task newTask = createTestTask();
            newTask.setAssigneeId(newAssigneeId);

            // When
            taskHistoryService.recordChanges(oldTask, newTask, userId);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<TaskHistory>> captor = ArgumentCaptor.forClass(List.class);
            verify(taskHistoryRepository).saveAll(captor.capture());

            List<TaskHistory> savedHistories = captor.getValue();
            assertThat(savedHistories).hasSize(1);
            assertThat(savedHistories.get(0).getFieldChanged()).isEqualTo(TaskHistoryField.ASSIGNEE);
            assertThat(savedHistories.get(0).getOldValue()).isEqualTo(userId.toString());
            assertThat(savedHistories.get(0).getNewValue()).isEqualTo(newAssigneeId.toString());
        }

        @Test
        @DisplayName("Should record multiple changes")
        void shouldRecordMultipleChanges() {
            // Given
            Task oldTask = createTestTask();
            Task newTask = createTestTask();
            newTask.setTitle("New Title");
            newTask.setStatus(TaskStatus.DONE);
            newTask.setPriority(TaskPriority.URGENT);

            // When
            taskHistoryService.recordChanges(oldTask, newTask, userId);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<TaskHistory>> captor = ArgumentCaptor.forClass(List.class);
            verify(taskHistoryRepository).saveAll(captor.capture());

            List<TaskHistory> savedHistories = captor.getValue();
            assertThat(savedHistories).hasSize(3);
            assertThat(savedHistories.stream().map(TaskHistory::getFieldChanged).toList())
                    .containsExactlyInAnyOrder(
                            TaskHistoryField.TITLE,
                            TaskHistoryField.STATUS,
                            TaskHistoryField.PRIORITY
                    );
        }

        @Test
        @DisplayName("Should not save when no changes")
        void shouldNotSaveWhenNoChanges() {
            // Given
            Task oldTask = createTestTask();
            Task newTask = createTestTask();

            // When
            taskHistoryService.recordChanges(oldTask, newTask, userId);

            // Then
            verify(taskHistoryRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should handle null old values")
        void shouldHandleNullOldValues() {
            // Given
            UUID newAssigneeId = UUID.randomUUID();
            Task oldTask = createTestTask();
            oldTask.setDescription(null);
            oldTask.setAssigneeId(null);

            Task newTask = createTestTask();
            newTask.setDescription(null);
            newTask.setAssigneeId(null);
            newTask.setDescription("New description");
            newTask.setAssigneeId(newAssigneeId);

            // When
            taskHistoryService.recordChanges(oldTask, newTask, userId);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<TaskHistory>> captor = ArgumentCaptor.forClass(List.class);
            verify(taskHistoryRepository).saveAll(captor.capture());

            List<TaskHistory> savedHistories = captor.getValue();
            assertThat(savedHistories).hasSize(2);

            TaskHistory descHistory = savedHistories.stream()
                    .filter(h -> h.getFieldChanged() == TaskHistoryField.DESCRIPTION)
                    .findFirst()
                    .orElseThrow();
            assertThat(descHistory.getOldValue()).isNull();
            assertThat(descHistory.getNewValue()).isEqualTo("New description");
        }

        private Task createTestTask() {
            return Task.builder()
                    .id(taskId)
                    .title("Test Task")
                    .description("Test description")
                    .status(TaskStatus.TODO)
                    .priority(TaskPriority.MEDIUM)
                    .organizationId(organizationId)
                    .assigneeId(userId)
                    .creatorId(userId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
    }

    @Nested
    @DisplayName("Record Single Change Tests")
    class RecordSingleChangeTests {

        @Test
        @DisplayName("Should record single field change")
        void shouldRecordSingleChange() {
            // When
            taskHistoryService.recordSingleChange(taskId, userId, TaskHistoryField.TITLE, "Old", "New");

            // Then
            ArgumentCaptor<TaskHistory> captor = ArgumentCaptor.forClass(TaskHistory.class);
            verify(taskHistoryRepository).save(captor.capture());

            TaskHistory saved = captor.getValue();
            assertThat(saved.getTaskId()).isEqualTo(taskId);
            assertThat(saved.getUserId()).isEqualTo(userId);
            assertThat(saved.getFieldChanged()).isEqualTo(TaskHistoryField.TITLE);
            assertThat(saved.getOldValue()).isEqualTo("Old");
            assertThat(saved.getNewValue()).isEqualTo("New");
            assertThat(saved.getChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should not save when values are equal")
        void shouldNotSaveWhenValuesEqual() {
            // When
            taskHistoryService.recordSingleChange(taskId, userId, TaskHistoryField.TITLE, "Same", "Same");

            // Then
            verify(taskHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not save when both values are null")
        void shouldNotSaveWhenBothNull() {
            // When
            taskHistoryService.recordSingleChange(taskId, userId, TaskHistoryField.TITLE, null, null);

            // Then
            verify(taskHistoryRepository, never()).save(any());
        }
    }
}
