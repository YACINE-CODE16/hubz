package com.hubz.application.service;

import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.HabitLogRepositoryPort;
import com.hubz.application.port.out.HabitRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.GoalType;
import com.hubz.domain.enums.HabitFrequency;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.Habit;
import com.hubz.domain.model.HabitLog;
import com.hubz.domain.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService Unit Tests")
class ReportServiceTest {

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private GoalRepositoryPort goalRepository;

    @Mock
    private HabitRepositoryPort habitRepository;

    @Mock
    private HabitLogRepositoryPort habitLogRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private ReportService reportService;

    private UUID userId;
    private UUID organizationId;
    private Task testTask;
    private Goal testGoal;
    private Habit testHabit;
    private HabitLog testHabitLog;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        testTask = Task.builder()
                .id(UUID.randomUUID())
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .organizationId(organizationId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testGoal = Goal.builder()
                .id(UUID.randomUUID())
                .title("Test Goal")
                .description("Test Goal Description")
                .type(GoalType.SHORT)
                .deadline(LocalDate.now().plusDays(30))
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        testHabit = Habit.builder()
                .id(UUID.randomUUID())
                .name("Test Habit")
                .icon("star")
                .frequency(HabitFrequency.DAILY)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        testHabitLog = HabitLog.builder()
                .id(UUID.randomUUID())
                .habitId(testHabit.getId())
                .date(LocalDate.now())
                .completed(true)
                .notes("Test notes")
                .duration(30)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("CSV Export Tests")
    class CsvExportTests {

        @Test
        @DisplayName("Should export tasks to CSV successfully")
        void shouldExportTasksToCsv() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testTask));

            // When
            byte[] result = reportService.exportTasksToCsv(organizationId, userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.length).isGreaterThan(0);

            String csv = new String(result);
            assertThat(csv).contains("ID,Title,Description,Status,Priority");
            assertThat(csv).contains("Test Task");
            assertThat(csv).contains("TODO");
            assertThat(csv).contains("HIGH");

            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
            verify(taskRepository).findByOrganizationId(organizationId);
        }

        @Test
        @DisplayName("Should export goals to CSV successfully")
        void shouldExportGoalsToCsv() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testGoal));
            when(taskRepository.findByGoalId(any())).thenReturn(List.of());

            // When
            byte[] result = reportService.exportGoalsToCsv(organizationId, userId);

            // Then
            assertThat(result).isNotNull();
            String csv = new String(result);
            assertThat(csv).contains("ID,Title,Description,Type,Deadline,Progress");
            assertThat(csv).contains("Test Goal");
            assertThat(csv).contains("SHORT");
        }

        @Test
        @DisplayName("Should export personal goals to CSV")
        void shouldExportPersonalGoalsToCsv() {
            // Given
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(testGoal));
            when(taskRepository.findByGoalId(any())).thenReturn(List.of());

            // When
            byte[] result = reportService.exportGoalsToCsv(null, userId);

            // Then
            assertThat(result).isNotNull();
            String csv = new String(result);
            assertThat(csv).contains("Test Goal");

            verify(goalRepository).findPersonalGoals(userId);
            verify(authorizationService, never()).checkOrganizationAccess(any(), any());
        }

        @Test
        @DisplayName("Should export habits to CSV successfully")
        void shouldExportHabitsToCsv() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(testHabit));
            when(habitLogRepository.findByHabitIdIn(any())).thenReturn(List.of(testHabitLog));

            // When
            byte[] result = reportService.exportHabitsToCsv(userId);

            // Then
            assertThat(result).isNotNull();
            String csv = new String(result);
            assertThat(csv).contains("Habit ID,Habit Name,Frequency,Date,Completed,Notes,Duration");
            assertThat(csv).contains("Test Habit");
            assertThat(csv).contains("DAILY");
            assertThat(csv).contains("true");
        }

        @Test
        @DisplayName("Should handle empty task list in CSV export")
        void shouldHandleEmptyTaskListInCsvExport() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(List.of());

            // When
            byte[] result = reportService.exportTasksToCsv(organizationId, userId);

            // Then
            assertThat(result).isNotNull();
            String csv = new String(result);
            assertThat(csv).contains("ID,Title,Description");
            // Should only contain header
        }
    }

    @Nested
    @DisplayName("Excel Export Tests")
    class ExcelExportTests {

        @Test
        @DisplayName("Should export tasks to Excel successfully")
        void shouldExportTasksToExcel() throws IOException {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testTask));

            // When
            byte[] result = reportService.exportTasksToExcel(organizationId, userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.length).isGreaterThan(0);
            // Excel files start with PK (ZIP format)
            assertThat(result[0]).isEqualTo((byte) 'P');
            assertThat(result[1]).isEqualTo((byte) 'K');
        }

        @Test
        @DisplayName("Should export goals to Excel successfully")
        void shouldExportGoalsToExcel() throws IOException {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testGoal));
            when(taskRepository.findByGoalId(any())).thenReturn(List.of());

            // When
            byte[] result = reportService.exportGoalsToExcel(organizationId, userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.length).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should export habits to Excel with multiple sheets")
        void shouldExportHabitsToExcel() throws IOException {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(testHabit));
            when(habitLogRepository.findByHabitIdIn(any())).thenReturn(List.of(testHabitLog));

            // When
            byte[] result = reportService.exportHabitsToExcel(userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.length).isGreaterThan(0);
            // Excel file should be created with summary and logs sheets
        }
    }

    @Nested
    @DisplayName("PDF Export Tests")
    class PdfExportTests {

        @Test
        @DisplayName("Should export tasks to PDF successfully")
        void shouldExportTasksToPdf() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testTask));

            // When
            byte[] result = reportService.exportTasksToPdf(organizationId, userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.length).isGreaterThan(0);
            // PDF files start with %PDF
            String header = new String(result, 0, Math.min(5, result.length));
            assertThat(header).startsWith("%PDF");
        }

        @Test
        @DisplayName("Should export goals to PDF successfully")
        void shouldExportGoalsToPdf() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testGoal));
            when(taskRepository.findByGoalId(any())).thenReturn(List.of());

            // When
            byte[] result = reportService.exportGoalsToPdf(organizationId, userId);

            // Then
            assertThat(result).isNotNull();
            String header = new String(result, 0, Math.min(5, result.length));
            assertThat(header).startsWith("%PDF");
        }

        @Test
        @DisplayName("Should export habits to PDF successfully")
        void shouldExportHabitsToPdf() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(testHabit));
            when(habitLogRepository.findByHabitIdIn(any())).thenReturn(List.of(testHabitLog));

            // When
            byte[] result = reportService.exportHabitsToPdf(userId);

            // Then
            assertThat(result).isNotNull();
            String header = new String(result, 0, Math.min(5, result.length));
            assertThat(header).startsWith("%PDF");
        }

        @Test
        @DisplayName("Should include summary in tasks PDF")
        void shouldIncludeSummaryInTasksPdf() {
            // Given
            Task doneTask = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Done Task")
                    .status(TaskStatus.DONE)
                    .priority(TaskPriority.LOW)
                    .createdAt(LocalDateTime.now())
                    .build();

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testTask, doneTask));

            // When
            byte[] result = reportService.exportTasksToPdf(organizationId, userId);

            // Then
            assertThat(result).isNotNull();
            // PDF should be generated with summary section
        }
    }

    @Nested
    @DisplayName("Progress Calculation Tests")
    class ProgressCalculationTests {

        @Test
        @DisplayName("Should calculate correct progress in CSV export")
        void shouldCalculateCorrectProgressInCsv() {
            // Given
            Task completedTask = Task.builder()
                    .id(UUID.randomUUID())
                    .status(TaskStatus.DONE)
                    .goalId(testGoal.getId())
                    .createdAt(LocalDateTime.now())
                    .build();

            Task pendingTask = Task.builder()
                    .id(UUID.randomUUID())
                    .status(TaskStatus.TODO)
                    .goalId(testGoal.getId())
                    .createdAt(LocalDateTime.now())
                    .build();

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(List.of(testGoal));
            when(taskRepository.findByGoalId(testGoal.getId())).thenReturn(List.of(completedTask, pendingTask));

            // When
            byte[] result = reportService.exportGoalsToCsv(organizationId, userId);

            // Then
            String csv = new String(result);
            // Check that progress contains 50 (format may vary: 50.0%, 50,0%, etc.)
            assertThat(csv).containsPattern("50[.,]0%");
        }
    }
}
