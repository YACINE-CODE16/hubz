package com.hubz.application.service;

import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.HabitLogRepositoryPort;
import com.hubz.application.port.out.HabitRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.Habit;
import com.hubz.domain.model.HabitLog;
import com.hubz.domain.model.Task;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TaskRepositoryPort taskRepository;
    private final GoalRepositoryPort goalRepository;
    private final HabitRepositoryPort habitRepository;
    private final HabitLogRepositoryPort habitLogRepository;
    private final AuthorizationService authorizationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================== CSV EXPORTS ====================

    public byte[] exportTasksToCsv(UUID organizationId, UUID userId) {
        authorizationService.checkOrganizationAccess(organizationId, userId);
        List<Task> tasks = taskRepository.findByOrganizationId(organizationId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            // BOM for Excel UTF-8 compatibility
            writer.write('\ufeff');
            // Header
            writer.println("ID,Title,Description,Status,Priority,Due Date,Created At,Updated At");

            for (Task task : tasks) {
                writer.printf("%s,\"%s\",\"%s\",%s,%s,%s,%s,%s%n",
                        task.getId(),
                        escapeCsv(task.getTitle()),
                        escapeCsv(task.getDescription()),
                        task.getStatus(),
                        task.getPriority(),
                        task.getDueDate() != null ? task.getDueDate().format(DATE_FORMATTER) : "",
                        task.getCreatedAt().format(DATE_FORMATTER),
                        task.getUpdatedAt() != null ? task.getUpdatedAt().format(DATE_FORMATTER) : ""
                );
            }
        }
        return baos.toByteArray();
    }

    public byte[] exportGoalsToCsv(UUID organizationId, UUID userId) {
        List<Goal> goals;
        if (organizationId != null) {
            authorizationService.checkOrganizationAccess(organizationId, userId);
            goals = goalRepository.findByOrganizationId(organizationId);
        } else {
            goals = goalRepository.findPersonalGoals(userId);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            writer.write('\ufeff');
            writer.println("ID,Title,Description,Type,Deadline,Progress,Created At");

            for (Goal goal : goals) {
                List<Task> tasks = taskRepository.findByGoalId(goal.getId());
                int total = tasks.size();
                int completed = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
                double progress = total > 0 ? (double) completed / total * 100 : 0;

                writer.printf("%s,\"%s\",\"%s\",%s,%s,%.1f%%,%s%n",
                        goal.getId(),
                        escapeCsv(goal.getTitle()),
                        escapeCsv(goal.getDescription()),
                        goal.getType(),
                        goal.getDeadline() != null ? goal.getDeadline().format(DATE_FORMATTER) : "",
                        progress,
                        goal.getCreatedAt().format(DATE_FORMATTER)
                );
            }
        }
        return baos.toByteArray();
    }

    public byte[] exportHabitsToCsv(UUID userId) {
        List<Habit> habits = habitRepository.findByUserId(userId);
        List<UUID> habitIds = habits.stream().map(Habit::getId).toList();
        List<HabitLog> allLogs = habitLogRepository.findByHabitIdIn(habitIds);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            writer.write('\ufeff');
            writer.println("Habit ID,Habit Name,Frequency,Date,Completed,Notes,Duration");

            for (Habit habit : habits) {
                List<HabitLog> logs = allLogs.stream()
                        .filter(log -> log.getHabitId().equals(habit.getId()))
                        .toList();

                for (HabitLog log : logs) {
                    writer.printf("%s,\"%s\",%s,%s,%s,\"%s\",%s%n",
                            habit.getId(),
                            escapeCsv(habit.getName()),
                            habit.getFrequency(),
                            log.getDate().format(DATE_FORMATTER),
                            log.getCompleted(),
                            escapeCsv(log.getNotes()),
                            log.getDuration() != null ? log.getDuration() : ""
                    );
                }
            }
        }
        return baos.toByteArray();
    }

    // ==================== EXCEL EXPORTS ====================

    public byte[] exportTasksToExcel(UUID organizationId, UUID userId) throws IOException {
        authorizationService.checkOrganizationAccess(organizationId, userId);
        List<Task> tasks = taskRepository.findByOrganizationId(organizationId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tasks");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Title", "Description", "Status", "Priority", "Due Date", "Created At", "Updated At"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (Task task : tasks) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(task.getId().toString());
                row.createCell(1).setCellValue(task.getTitle());
                row.createCell(2).setCellValue(task.getDescription() != null ? task.getDescription() : "");
                row.createCell(3).setCellValue(task.getStatus().name());
                row.createCell(4).setCellValue(task.getPriority().name());
                row.createCell(5).setCellValue(task.getDueDate() != null ? task.getDueDate().format(DATE_FORMATTER) : "");
                row.createCell(6).setCellValue(task.getCreatedAt().format(DATE_FORMATTER));
                row.createCell(7).setCellValue(task.getUpdatedAt() != null ? task.getUpdatedAt().format(DATE_FORMATTER) : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportGoalsToExcel(UUID organizationId, UUID userId) throws IOException {
        List<Goal> goals;
        if (organizationId != null) {
            authorizationService.checkOrganizationAccess(organizationId, userId);
            goals = goalRepository.findByOrganizationId(organizationId);
        } else {
            goals = goalRepository.findPersonalGoals(userId);
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Goals");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Title", "Description", "Type", "Deadline", "Total Tasks", "Completed Tasks", "Progress", "Created At"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Goal goal : goals) {
                List<Task> tasks = taskRepository.findByGoalId(goal.getId());
                int total = tasks.size();
                int completed = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
                double progress = total > 0 ? (double) completed / total * 100 : 0;

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(goal.getId().toString());
                row.createCell(1).setCellValue(goal.getTitle());
                row.createCell(2).setCellValue(goal.getDescription() != null ? goal.getDescription() : "");
                row.createCell(3).setCellValue(goal.getType().name());
                row.createCell(4).setCellValue(goal.getDeadline() != null ? goal.getDeadline().format(DATE_FORMATTER) : "");
                row.createCell(5).setCellValue(total);
                row.createCell(6).setCellValue(completed);
                row.createCell(7).setCellValue(String.format("%.1f%%", progress));
                row.createCell(8).setCellValue(goal.getCreatedAt().format(DATE_FORMATTER));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportHabitsToExcel(UUID userId) throws IOException {
        List<Habit> habits = habitRepository.findByUserId(userId);
        List<UUID> habitIds = habits.stream().map(Habit::getId).toList();
        List<HabitLog> allLogs = habitLogRepository.findByHabitIdIn(habitIds);

        try (Workbook workbook = new XSSFWorkbook()) {
            // Habits summary sheet
            Sheet summarySheet = workbook.createSheet("Habits Summary");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = summarySheet.createRow(0);
            String[] headers = {"ID", "Name", "Icon", "Frequency", "Total Logs", "Completed", "Completion Rate"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Habit habit : habits) {
                List<HabitLog> logs = allLogs.stream()
                        .filter(log -> log.getHabitId().equals(habit.getId()))
                        .toList();
                long totalLogs = logs.size();
                long completedLogs = logs.stream().filter(l -> l.getCompleted() != null && l.getCompleted()).count();
                double rate = totalLogs > 0 ? (double) completedLogs / totalLogs * 100 : 0;

                Row row = summarySheet.createRow(rowNum++);
                row.createCell(0).setCellValue(habit.getId().toString());
                row.createCell(1).setCellValue(habit.getName());
                row.createCell(2).setCellValue(habit.getIcon());
                row.createCell(3).setCellValue(habit.getFrequency().name());
                row.createCell(4).setCellValue(totalLogs);
                row.createCell(5).setCellValue(completedLogs);
                row.createCell(6).setCellValue(String.format("%.1f%%", rate));
            }

            for (int i = 0; i < headers.length; i++) {
                summarySheet.autoSizeColumn(i);
            }

            // Logs detail sheet
            Sheet logsSheet = workbook.createSheet("Habit Logs");

            Row logHeaderRow = logsSheet.createRow(0);
            String[] logHeaders = {"Habit Name", "Date", "Completed", "Notes", "Duration (min)"};
            for (int i = 0; i < logHeaders.length; i++) {
                Cell cell = logHeaderRow.createCell(i);
                cell.setCellValue(logHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int logRowNum = 1;
            for (Habit habit : habits) {
                List<HabitLog> logs = allLogs.stream()
                        .filter(log -> log.getHabitId().equals(habit.getId()))
                        .toList();

                for (HabitLog log : logs) {
                    Row row = logsSheet.createRow(logRowNum++);
                    row.createCell(0).setCellValue(habit.getName());
                    row.createCell(1).setCellValue(log.getDate().format(DATE_FORMATTER));
                    row.createCell(2).setCellValue(log.getCompleted() != null && log.getCompleted() ? "Yes" : "No");
                    row.createCell(3).setCellValue(log.getNotes() != null ? log.getNotes() : "");
                    row.createCell(4).setCellValue(log.getDuration() != null ? log.getDuration() : 0);
                }
            }

            for (int i = 0; i < logHeaders.length; i++) {
                logsSheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    // ==================== PDF EXPORTS ====================

    public byte[] exportTasksToPdf(UUID organizationId, UUID userId) {
        authorizationService.checkOrganizationAccess(organizationId, userId);
        List<Task> tasks = taskRepository.findByOrganizationId(organizationId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Title
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.DARK_GRAY);
            Paragraph title = new Paragraph("Tasks Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Date
            Font dateFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
            Paragraph date = new Paragraph("Generated on: " + LocalDate.now().format(DATE_FORMATTER), dateFont);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);

            // Table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 3, 1.5f, 1.5f, 2, 2});

            // Header
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            String[] headers = {"Title", "Description", "Status", "Priority", "Due Date", "Created"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(59, 130, 246));
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Data
            Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
            for (Task task : tasks) {
                table.addCell(createPdfCell(task.getTitle(), dataFont));
                table.addCell(createPdfCell(truncate(task.getDescription(), 50), dataFont));
                table.addCell(createPdfCell(task.getStatus().name(), dataFont));
                table.addCell(createPdfCell(task.getPriority().name(), dataFont));
                table.addCell(createPdfCell(task.getDueDate() != null ? task.getDueDate().format(DATE_FORMATTER) : "-", dataFont));
                table.addCell(createPdfCell(task.getCreatedAt().format(DATE_FORMATTER), dataFont));
            }

            document.add(table);

            // Summary
            Font summaryFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            Paragraph summary = new Paragraph("\nSummary", summaryFont);
            summary.setSpacingBefore(20);
            document.add(summary);

            long todoCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
            long inProgressCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
            long doneCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();

            Font statsFont = new Font(Font.HELVETICA, 10);
            document.add(new Paragraph(String.format("Total Tasks: %d", tasks.size()), statsFont));
            document.add(new Paragraph(String.format("To Do: %d | In Progress: %d | Done: %d", todoCount, inProgressCount, doneCount), statsFont));

        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF", e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    public byte[] exportGoalsToPdf(UUID organizationId, UUID userId) {
        List<Goal> goals;
        String reportTitle;
        if (organizationId != null) {
            authorizationService.checkOrganizationAccess(organizationId, userId);
            goals = goalRepository.findByOrganizationId(organizationId);
            reportTitle = "Organization Goals Report";
        } else {
            goals = goalRepository.findPersonalGoals(userId);
            reportTitle = "Personal Goals Report";
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.DARK_GRAY);
            Paragraph title = new Paragraph(reportTitle, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            Font dateFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
            Paragraph date = new Paragraph("Generated on: " + LocalDate.now().format(DATE_FORMATTER), dateFont);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 1.5f, 2, 2, 1.5f});

            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            String[] headers = {"Title", "Type", "Deadline", "Progress", "Status"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(34, 197, 94));
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
            for (Goal goal : goals) {
                List<Task> tasks = taskRepository.findByGoalId(goal.getId());
                int total = tasks.size();
                int completed = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
                double progress = total > 0 ? (double) completed / total * 100 : 0;
                String status = progress >= 100 ? "Completed" : progress >= 50 ? "On Track" : "In Progress";

                table.addCell(createPdfCell(goal.getTitle(), dataFont));
                table.addCell(createPdfCell(goal.getType().name(), dataFont));
                table.addCell(createPdfCell(goal.getDeadline() != null ? goal.getDeadline().format(DATE_FORMATTER) : "-", dataFont));
                table.addCell(createPdfCell(String.format("%.1f%% (%d/%d)", progress, completed, total), dataFont));
                table.addCell(createPdfCell(status, dataFont));
            }

            document.add(table);

        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF", e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    public byte[] exportHabitsToPdf(UUID userId) {
        List<Habit> habits = habitRepository.findByUserId(userId);
        List<UUID> habitIds = habits.stream().map(Habit::getId).toList();
        List<HabitLog> allLogs = habitLogRepository.findByHabitIdIn(habitIds);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.DARK_GRAY);
            Paragraph title = new Paragraph("Habits Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            Font dateFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
            Paragraph date = new Paragraph("Generated on: " + LocalDate.now().format(DATE_FORMATTER), dateFont);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 1.5f, 1.5f, 1.5f, 2});

            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            String[] headers = {"Habit Name", "Frequency", "Total Logs", "Completed", "Rate"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(249, 115, 22));
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
            for (Habit habit : habits) {
                List<HabitLog> logs = allLogs.stream()
                        .filter(log -> log.getHabitId().equals(habit.getId()))
                        .toList();
                long totalLogs = logs.size();
                long completedLogs = logs.stream().filter(l -> l.getCompleted() != null && l.getCompleted()).count();
                double rate = totalLogs > 0 ? (double) completedLogs / totalLogs * 100 : 0;

                table.addCell(createPdfCell(habit.getName(), dataFont));
                table.addCell(createPdfCell(habit.getFrequency().name(), dataFont));
                table.addCell(createPdfCell(String.valueOf(totalLogs), dataFont));
                table.addCell(createPdfCell(String.valueOf(completedLogs), dataFont));
                table.addCell(createPdfCell(String.format("%.1f%%", rate), dataFont));
            }

            document.add(table);

        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF", e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    // ==================== HELPER METHODS ====================

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"").replace("\n", " ").replace("\r", "");
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
    }

    private PdfPCell createPdfCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }
}
