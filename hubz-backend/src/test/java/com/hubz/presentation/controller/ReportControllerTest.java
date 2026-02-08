package com.hubz.presentation.controller;

import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.ReportService;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.OrganizationNotFoundException;
import com.hubz.domain.model.User;
import com.hubz.infrastructure.config.CorsProperties;
import com.hubz.infrastructure.security.JwtAuthenticationFilter;
import com.hubz.infrastructure.security.JwtService;
import com.hubz.presentation.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = ReportController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, JwtService.class, CorsProperties.class}
        )
)
@Import(GlobalExceptionHandler.class)
@DisplayName("ReportController Unit Tests")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private UUID orgId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        mockAuth = mock(Authentication.class);
        when(mockAuth.getName()).thenReturn("test@example.com");
        when(userRepositoryPort.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }

    @Nested
    @DisplayName("Tasks Export Endpoints")
    class TasksExportTests {

        @Test
        @DisplayName("GET /api/reports/organizations/{orgId}/tasks/csv - Should export tasks to CSV")
        void shouldExportTasksToCsv() throws Exception {
            // Given
            byte[] csvData = "id,title,status\n1,Task1,TODO".getBytes();
            when(reportService.exportTasksToCsv(orgId, userId)).thenReturn(csvData);

            // When & Then
            mockMvc.perform(get("/api/reports/organizations/{orgId}/tasks/csv", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/csv"))
                    .andExpect(header().exists("Content-Disposition"));

            verify(reportService).exportTasksToCsv(orgId, userId);
        }

        @Test
        @DisplayName("GET /api/reports/organizations/{orgId}/tasks/excel - Should export tasks to Excel")
        void shouldExportTasksToExcel() throws Exception {
            // Given
            byte[] excelData = "Excel content".getBytes();
            when(reportService.exportTasksToExcel(orgId, userId)).thenReturn(excelData);

            // When & Then
            mockMvc.perform(get("/api/reports/organizations/{orgId}/tasks/excel", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .andExpect(header().exists("Content-Disposition"));

            verify(reportService).exportTasksToExcel(orgId, userId);
        }

        @Test
        @DisplayName("GET /api/reports/organizations/{orgId}/tasks/pdf - Should export tasks to PDF")
        void shouldExportTasksToPdf() throws Exception {
            // Given
            byte[] pdfData = "PDF content".getBytes();
            when(reportService.exportTasksToPdf(orgId, userId)).thenReturn(pdfData);

            // When & Then
            mockMvc.perform(get("/api/reports/organizations/{orgId}/tasks/pdf", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().exists("Content-Disposition"));

            verify(reportService).exportTasksToPdf(orgId, userId);
        }

        @Test
        @DisplayName("Should return 404 when organization not found")
        void shouldReturn404WhenOrgNotFound() throws Exception {
            // Given
            when(reportService.exportTasksToCsv(orgId, userId))
                    .thenThrow(new OrganizationNotFoundException(orgId));

            // When & Then
            mockMvc.perform(get("/api/reports/organizations/{orgId}/tasks/csv", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not authorized")
        void shouldReturn403WhenNotAuthorized() throws Exception {
            // Given
            when(reportService.exportTasksToCsv(orgId, userId))
                    .thenThrow(new AccessDeniedException("Not authorized"));

            // When & Then
            mockMvc.perform(get("/api/reports/organizations/{orgId}/tasks/csv", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Organization Goals Export Endpoints")
    class OrganizationGoalsExportTests {

        @Test
        @DisplayName("GET /api/reports/organizations/{orgId}/goals/csv - Should export goals to CSV")
        void shouldExportGoalsToCsv() throws Exception {
            // Given
            byte[] csvData = "id,title,progress\n1,Goal1,50".getBytes();
            when(reportService.exportGoalsToCsv(orgId, userId)).thenReturn(csvData);

            // When & Then
            mockMvc.perform(get("/api/reports/organizations/{orgId}/goals/csv", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/csv"));

            verify(reportService).exportGoalsToCsv(orgId, userId);
        }

        @Test
        @DisplayName("GET /api/reports/organizations/{orgId}/goals/excel - Should export goals to Excel")
        void shouldExportGoalsToExcel() throws Exception {
            // Given
            byte[] excelData = "Excel content".getBytes();
            when(reportService.exportGoalsToExcel(orgId, userId)).thenReturn(excelData);

            // When & Then
            mockMvc.perform(get("/api/reports/organizations/{orgId}/goals/excel", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

            verify(reportService).exportGoalsToExcel(orgId, userId);
        }

        @Test
        @DisplayName("GET /api/reports/organizations/{orgId}/goals/pdf - Should export goals to PDF")
        void shouldExportGoalsToPdf() throws Exception {
            // Given
            byte[] pdfData = "PDF content".getBytes();
            when(reportService.exportGoalsToPdf(orgId, userId)).thenReturn(pdfData);

            // When & Then
            mockMvc.perform(get("/api/reports/organizations/{orgId}/goals/pdf", orgId)
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));

            verify(reportService).exportGoalsToPdf(orgId, userId);
        }
    }

    @Nested
    @DisplayName("Personal Goals Export Endpoints")
    class PersonalGoalsExportTests {

        @Test
        @DisplayName("GET /api/reports/users/me/goals/csv - Should export personal goals to CSV")
        void shouldExportPersonalGoalsToCsv() throws Exception {
            // Given
            byte[] csvData = "id,title,progress\n1,Personal Goal,75".getBytes();
            when(reportService.exportGoalsToCsv(null, userId)).thenReturn(csvData);

            // When & Then
            mockMvc.perform(get("/api/reports/users/me/goals/csv")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/csv"));

            verify(reportService).exportGoalsToCsv(null, userId);
        }

        @Test
        @DisplayName("GET /api/reports/users/me/goals/excel - Should export personal goals to Excel")
        void shouldExportPersonalGoalsToExcel() throws Exception {
            // Given
            byte[] excelData = "Excel content".getBytes();
            when(reportService.exportGoalsToExcel(null, userId)).thenReturn(excelData);

            // When & Then
            mockMvc.perform(get("/api/reports/users/me/goals/excel")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

            verify(reportService).exportGoalsToExcel(null, userId);
        }

        @Test
        @DisplayName("GET /api/reports/users/me/goals/pdf - Should export personal goals to PDF")
        void shouldExportPersonalGoalsToPdf() throws Exception {
            // Given
            byte[] pdfData = "PDF content".getBytes();
            when(reportService.exportGoalsToPdf(null, userId)).thenReturn(pdfData);

            // When & Then
            mockMvc.perform(get("/api/reports/users/me/goals/pdf")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));

            verify(reportService).exportGoalsToPdf(null, userId);
        }
    }

    @Nested
    @DisplayName("Habits Export Endpoints")
    class HabitsExportTests {

        @Test
        @DisplayName("GET /api/reports/users/me/habits/csv - Should export habits to CSV")
        void shouldExportHabitsToCsv() throws Exception {
            // Given
            byte[] csvData = "id,name,streak\n1,Morning Run,15".getBytes();
            when(reportService.exportHabitsToCsv(userId)).thenReturn(csvData);

            // When & Then
            mockMvc.perform(get("/api/reports/users/me/habits/csv")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/csv"));

            verify(reportService).exportHabitsToCsv(userId);
        }

        @Test
        @DisplayName("GET /api/reports/users/me/habits/excel - Should export habits to Excel")
        void shouldExportHabitsToExcel() throws Exception {
            // Given
            byte[] excelData = "Excel content".getBytes();
            when(reportService.exportHabitsToExcel(userId)).thenReturn(excelData);

            // When & Then
            mockMvc.perform(get("/api/reports/users/me/habits/excel")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

            verify(reportService).exportHabitsToExcel(userId);
        }

        @Test
        @DisplayName("GET /api/reports/users/me/habits/pdf - Should export habits to PDF")
        void shouldExportHabitsToPdf() throws Exception {
            // Given
            byte[] pdfData = "PDF content".getBytes();
            when(reportService.exportHabitsToPdf(userId)).thenReturn(pdfData);

            // When & Then
            mockMvc.perform(get("/api/reports/users/me/habits/pdf")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));

            verify(reportService).exportHabitsToPdf(userId);
        }
    }
}
