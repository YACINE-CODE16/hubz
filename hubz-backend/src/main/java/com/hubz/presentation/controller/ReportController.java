package com.hubz.presentation.controller;

import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.ReportService;
import com.hubz.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserRepositoryPort userRepositoryPort;

    private static final String CSV_CONTENT_TYPE = "text/csv";
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    // ==================== TASKS EXPORTS ====================

    @GetMapping("/organizations/{orgId}/tasks/csv")
    public ResponseEntity<byte[]> exportTasksToCsv(
            @PathVariable UUID orgId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        byte[] data = reportService.exportTasksToCsv(orgId, userId);
        return createResponse(data, CSV_CONTENT_TYPE, "tasks_" + formatDate() + ".csv");
    }

    @GetMapping("/organizations/{orgId}/tasks/excel")
    public ResponseEntity<byte[]> exportTasksToExcel(
            @PathVariable UUID orgId,
            Authentication authentication) throws IOException {
        UUID userId = resolveUserId(authentication);
        byte[] data = reportService.exportTasksToExcel(orgId, userId);
        return createResponse(data, EXCEL_CONTENT_TYPE, "tasks_" + formatDate() + ".xlsx");
    }

    @GetMapping("/organizations/{orgId}/tasks/pdf")
    public ResponseEntity<byte[]> exportTasksToPdf(
            @PathVariable UUID orgId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        byte[] data = reportService.exportTasksToPdf(orgId, userId);
        return createResponse(data, PDF_CONTENT_TYPE, "tasks_" + formatDate() + ".pdf");
    }

    // ==================== GOALS EXPORTS (Organization) ====================

    @GetMapping("/organizations/{orgId}/goals/csv")
    public ResponseEntity<byte[]> exportOrganizationGoalsToCsv(
            @PathVariable UUID orgId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        byte[] data = reportService.exportGoalsToCsv(orgId, userId);
        return createResponse(data, CSV_CONTENT_TYPE, "goals_" + formatDate() + ".csv");
    }

    @GetMapping("/organizations/{orgId}/goals/excel")
    public ResponseEntity<byte[]> exportOrganizationGoalsToExcel(
            @PathVariable UUID orgId,
            Authentication authentication) throws IOException {
        UUID userId = resolveUserId(authentication);
        byte[] data = reportService.exportGoalsToExcel(orgId, userId);
        return createResponse(data, EXCEL_CONTENT_TYPE, "goals_" + formatDate() + ".xlsx");
    }

    @GetMapping("/organizations/{orgId}/goals/pdf")
    public ResponseEntity<byte[]> exportOrganizationGoalsToPdf(
            @PathVariable UUID orgId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        byte[] data = reportService.exportGoalsToPdf(orgId, userId);
        return createResponse(data, PDF_CONTENT_TYPE, "goals_" + formatDate() + ".pdf");
    }

    // ==================== GOALS EXPORTS (Personal) ====================

    @GetMapping("/users/me/goals/csv")
    public ResponseEntity<byte[]> exportPersonalGoalsToCsv(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        byte[] data = reportService.exportGoalsToCsv(null, userId);
        return createResponse(data, CSV_CONTENT_TYPE, "personal_goals_" + formatDate() + ".csv");
    }

    @GetMapping("/users/me/goals/excel")
    public ResponseEntity<byte[]> exportPersonalGoalsToExcel(Authentication authentication) throws IOException {
        UUID userId = resolveUserId(authentication);
        byte[] data = reportService.exportGoalsToExcel(null, userId);
        return createResponse(data, EXCEL_CONTENT_TYPE, "personal_goals_" + formatDate() + ".xlsx");
    }

    @GetMapping("/users/me/goals/pdf")
    public ResponseEntity<byte[]> exportPersonalGoalsToPdf(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        byte[] data = reportService.exportGoalsToPdf(null, userId);
        return createResponse(data, PDF_CONTENT_TYPE, "personal_goals_" + formatDate() + ".pdf");
    }

    // ==================== HABITS EXPORTS ====================

    @GetMapping("/users/me/habits/csv")
    public ResponseEntity<byte[]> exportHabitsToCsv(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        byte[] data = reportService.exportHabitsToCsv(userId);
        return createResponse(data, CSV_CONTENT_TYPE, "habits_" + formatDate() + ".csv");
    }

    @GetMapping("/users/me/habits/excel")
    public ResponseEntity<byte[]> exportHabitsToExcel(Authentication authentication) throws IOException {
        UUID userId = resolveUserId(authentication);
        byte[] data = reportService.exportHabitsToExcel(userId);
        return createResponse(data, EXCEL_CONTENT_TYPE, "habits_" + formatDate() + ".xlsx");
    }

    @GetMapping("/users/me/habits/pdf")
    public ResponseEntity<byte[]> exportHabitsToPdf(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        byte[] data = reportService.exportHabitsToPdf(userId);
        return createResponse(data, PDF_CONTENT_TYPE, "habits_" + formatDate() + ".pdf");
    }

    // ==================== HELPER METHODS ====================

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }

    private String formatDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private ResponseEntity<byte[]> createResponse(byte[] data, String contentType, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(data.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }
}
