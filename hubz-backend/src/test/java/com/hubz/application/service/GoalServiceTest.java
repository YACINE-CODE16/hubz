package com.hubz.application.service;

import com.hubz.application.dto.request.CreateGoalRequest;
import com.hubz.application.dto.request.UpdateGoalRequest;
import com.hubz.application.dto.response.GoalResponse;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.GoalType;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.exception.GoalNotFoundException;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoalService Unit Tests")
class GoalServiceTest {

    @Mock
    private GoalRepositoryPort goalRepository;

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private GoalService goalService;

    private UUID organizationId;
    private UUID userId;
    private UUID goalId;
    private Goal testOrgGoal;
    private Goal testPersonalGoal;
    private Task testTask;
    private Task completedTask;
    private CreateGoalRequest createRequest;
    private UpdateGoalRequest updateRequest;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        goalId = UUID.randomUUID();

        testOrgGoal = Goal.builder()
                .id(goalId)
                .title("Test Organization Goal")
                .description("Test description")
                .type(GoalType.MEDIUM)
                .deadline(LocalDate.now().plusMonths(6))
                .organizationId(organizationId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testPersonalGoal = Goal.builder()
                .id(UUID.randomUUID())
                .title("Test Personal Goal")
                .description("Personal description")
                .type(GoalType.SHORT)
                .deadline(LocalDate.now().plusMonths(1))
                .organizationId(null)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Test Task")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.MEDIUM)
                .organizationId(organizationId)
                .goalId(goalId)
                .creatorId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        completedTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Completed Task")
                .status(TaskStatus.DONE)
                .priority(TaskPriority.MEDIUM)
                .organizationId(organizationId)
                .goalId(goalId)
                .creatorId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateGoalRequest.builder()
                .title("Test Goal")
                .description("Test description")
                .type(GoalType.MEDIUM)
                .deadline(LocalDate.now().plusMonths(6))
                .build();

        updateRequest = UpdateGoalRequest.builder()
                .title("Updated Goal")
                .description("Updated description")
                .type(GoalType.LONG)
                .deadline(LocalDate.now().plusYears(1))
                .build();
    }

    @Nested
    @DisplayName("Create Goal Tests")
    class CreateTests {

        @Test
        @DisplayName("Should successfully create organization goal")
        void shouldCreateOrganizationGoal() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.save(any(Goal.class))).thenReturn(testOrgGoal);
            when(taskRepository.findByGoalId(any())).thenReturn(List.of());

            // When
            GoalResponse response = goalService.create(createRequest, organizationId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo(testOrgGoal.getTitle());
            assertThat(response.getOrganizationId()).isEqualTo(organizationId);
            assertThat(response.getUserId()).isEqualTo(userId);

            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(goalRepository).save(any(Goal.class));
        }

        @Test
        @DisplayName("Should successfully create personal goal without authorization check")
        void shouldCreatePersonalGoal() {
            // Given
            when(goalRepository.save(any(Goal.class))).thenReturn(testPersonalGoal);
            when(taskRepository.findByGoalId(any())).thenReturn(List.of());

            // When
            GoalResponse response = goalService.create(createRequest, null, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOrganizationId()).isNull();
            assertThat(response.getUserId()).isEqualTo(userId);

            verify(authorizationService, never()).checkOrganizationAccess(any(), any());
            verify(goalRepository).save(any(Goal.class));
        }

        @Test
        @DisplayName("Should set timestamps when creating goal")
        void shouldSetTimestamps() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            when(goalRepository.save(goalCaptor.capture())).thenReturn(testOrgGoal);
            when(taskRepository.findByGoalId(any())).thenReturn(List.of());

            // When
            goalService.create(createRequest, organizationId, userId);

            // Then
            Goal savedGoal = goalCaptor.getValue();
            assertThat(savedGoal.getCreatedAt()).isNotNull();
            assertThat(savedGoal.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should generate UUID for new goal")
        void shouldGenerateUUID() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            when(goalRepository.save(goalCaptor.capture())).thenReturn(testOrgGoal);
            when(taskRepository.findByGoalId(any())).thenReturn(List.of());

            // When
            goalService.create(createRequest, organizationId, userId);

            // Then
            Goal savedGoal = goalCaptor.getValue();
            assertThat(savedGoal.getId()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when user has no access to organization")
        void shouldThrowExceptionWhenNoAccess() {
            // Given
            doThrow(new RuntimeException("No access"))
                    .when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> goalService.create(createRequest, organizationId, userId))
                    .isInstanceOf(RuntimeException.class);
            verify(goalRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Goals By Organization Tests")
    class GetByOrganizationTests {

        @Test
        @DisplayName("Should successfully get goals by organization")
        void shouldGetGoalsByOrganization() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testOrgGoal));
            when(taskRepository.findByGoalId(goalId)).thenReturn(List.of(testTask, completedTask));

            // When
            List<GoalResponse> goals = goalService.getByOrganization(organizationId, userId);

            // Then
            assertThat(goals).hasSize(1);
            assertThat(goals.get(0).getTitle()).isEqualTo(testOrgGoal.getTitle());
            assertThat(goals.get(0).getTotalTasks()).isEqualTo(2);
            assertThat(goals.get(0).getCompletedTasks()).isEqualTo(1);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(goalRepository).findByOrganizationId(organizationId);
        }

        @Test
        @DisplayName("Should return empty list when no goals exist")
        void shouldReturnEmptyListWhenNoGoals() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(List.of());

            // When
            List<GoalResponse> goals = goalService.getByOrganization(organizationId, userId);

            // Then
            assertThat(goals).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Personal Goals Tests")
    class GetPersonalGoalsTests {

        @Test
        @DisplayName("Should successfully get personal goals")
        void shouldGetPersonalGoals() {
            // Given
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(testPersonalGoal));
            when(taskRepository.findByGoalId(testPersonalGoal.getId())).thenReturn(List.of());

            // When
            List<GoalResponse> goals = goalService.getPersonalGoals(userId);

            // Then
            assertThat(goals).hasSize(1);
            assertThat(goals.get(0).getTitle()).isEqualTo(testPersonalGoal.getTitle());
            assertThat(goals.get(0).getOrganizationId()).isNull();
            verify(goalRepository).findPersonalGoals(userId);
        }

        @Test
        @DisplayName("Should return empty list when user has no personal goals")
        void shouldReturnEmptyListWhenNoPersonalGoals() {
            // Given
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());

            // When
            List<GoalResponse> goals = goalService.getPersonalGoals(userId);

            // Then
            assertThat(goals).isEmpty();
        }
    }

    @Nested
    @DisplayName("Update Goal Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should successfully update organization goal")
        void shouldUpdateOrganizationGoal() {
            // Given
            when(goalRepository.findById(goalId)).thenReturn(Optional.of(testOrgGoal));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.save(any(Goal.class))).thenReturn(testOrgGoal);
            when(taskRepository.findByGoalId(goalId)).thenReturn(List.of());

            // When
            GoalResponse response = goalService.update(goalId, updateRequest, userId);

            // Then
            assertThat(response).isNotNull();
            verify(goalRepository).findById(goalId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(goalRepository).save(any(Goal.class));
        }

        @Test
        @DisplayName("Should successfully update personal goal by owner")
        void shouldUpdatePersonalGoalByOwner() {
            // Given
            when(goalRepository.findById(testPersonalGoal.getId())).thenReturn(Optional.of(testPersonalGoal));
            when(goalRepository.save(any(Goal.class))).thenReturn(testPersonalGoal);
            when(taskRepository.findByGoalId(testPersonalGoal.getId())).thenReturn(List.of());

            // When
            GoalResponse response = goalService.update(testPersonalGoal.getId(), updateRequest, userId);

            // Then
            assertThat(response).isNotNull();
            verify(authorizationService, never()).checkOrganizationAccess(any(), any());
            verify(goalRepository).save(any(Goal.class));
        }

        @Test
        @DisplayName("Should throw exception when goal not found")
        void shouldThrowExceptionWhenGoalNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(goalRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> goalService.update(nonExistentId, updateRequest, userId))
                    .isInstanceOf(GoalNotFoundException.class);
            verify(goalRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when updating personal goal by non-owner")
        void shouldThrowExceptionWhenNonOwnerUpdatesPersonalGoal() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            when(goalRepository.findById(testPersonalGoal.getId())).thenReturn(Optional.of(testPersonalGoal));

            // When & Then
            assertThatThrownBy(() -> goalService.update(testPersonalGoal.getId(), updateRequest, otherUserId))
                    .isInstanceOf(GoalNotFoundException.class);
            verify(goalRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update updatedAt timestamp")
        void shouldUpdateTimestamp() {
            // Given
            when(goalRepository.findById(goalId)).thenReturn(Optional.of(testOrgGoal));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            when(goalRepository.save(goalCaptor.capture())).thenReturn(testOrgGoal);
            when(taskRepository.findByGoalId(goalId)).thenReturn(List.of());

            // When
            goalService.update(goalId, updateRequest, userId);

            // Then
            Goal updatedGoal = goalCaptor.getValue();
            assertThat(updatedGoal.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should only update non-null fields")
        void shouldOnlyUpdateNonNullFields() {
            // Given
            UpdateGoalRequest partialUpdate = UpdateGoalRequest.builder()
                    .title("New Title")
                    .build();

            when(goalRepository.findById(goalId)).thenReturn(Optional.of(testOrgGoal));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
            when(goalRepository.save(goalCaptor.capture())).thenReturn(testOrgGoal);
            when(taskRepository.findByGoalId(goalId)).thenReturn(List.of());

            // When
            goalService.update(goalId, partialUpdate, userId);

            // Then
            Goal updatedGoal = goalCaptor.getValue();
            assertThat(updatedGoal.getTitle()).isEqualTo("New Title");
            assertThat(updatedGoal.getDescription()).isEqualTo(testOrgGoal.getDescription());
            assertThat(updatedGoal.getType()).isEqualTo(testOrgGoal.getType());
        }
    }

    @Nested
    @DisplayName("Delete Goal Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should successfully delete organization goal")
        void shouldDeleteOrganizationGoal() {
            // Given
            when(goalRepository.findById(goalId)).thenReturn(Optional.of(testOrgGoal));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            doNothing().when(goalRepository).deleteById(goalId);

            // When
            goalService.delete(goalId, userId);

            // Then
            verify(goalRepository).findById(goalId);
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(goalRepository).deleteById(goalId);
        }

        @Test
        @DisplayName("Should successfully delete personal goal by owner")
        void shouldDeletePersonalGoalByOwner() {
            // Given
            when(goalRepository.findById(testPersonalGoal.getId())).thenReturn(Optional.of(testPersonalGoal));
            doNothing().when(goalRepository).deleteById(testPersonalGoal.getId());

            // When
            goalService.delete(testPersonalGoal.getId(), userId);

            // Then
            verify(authorizationService, never()).checkOrganizationAccess(any(), any());
            verify(goalRepository).deleteById(testPersonalGoal.getId());
        }

        @Test
        @DisplayName("Should throw exception when goal not found")
        void shouldThrowExceptionWhenGoalNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(goalRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> goalService.delete(nonExistentId, userId))
                    .isInstanceOf(GoalNotFoundException.class);
            verify(goalRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw exception when deleting personal goal by non-owner")
        void shouldThrowExceptionWhenNonOwnerDeletesPersonalGoal() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            when(goalRepository.findById(testPersonalGoal.getId())).thenReturn(Optional.of(testPersonalGoal));

            // When & Then
            assertThatThrownBy(() -> goalService.delete(testPersonalGoal.getId(), otherUserId))
                    .isInstanceOf(GoalNotFoundException.class);
            verify(goalRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Task Count Tests")
    class TaskCountTests {

        @Test
        @DisplayName("Should correctly count total and completed tasks")
        void shouldCorrectlyCountTasks() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testOrgGoal));
            when(taskRepository.findByGoalId(goalId)).thenReturn(List.of(testTask, completedTask));

            // When
            List<GoalResponse> goals = goalService.getByOrganization(organizationId, userId);

            // Then
            assertThat(goals.get(0).getTotalTasks()).isEqualTo(2);
            assertThat(goals.get(0).getCompletedTasks()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return zero tasks when goal has no tasks")
        void shouldReturnZeroTasksWhenNoTasks() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testOrgGoal));
            when(taskRepository.findByGoalId(goalId)).thenReturn(List.of());

            // When
            List<GoalResponse> goals = goalService.getByOrganization(organizationId, userId);

            // Then
            assertThat(goals.get(0).getTotalTasks()).isEqualTo(0);
            assertThat(goals.get(0).getCompletedTasks()).isEqualTo(0);
        }
    }
}
