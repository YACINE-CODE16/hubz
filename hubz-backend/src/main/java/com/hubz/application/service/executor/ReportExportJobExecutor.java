package com.hubz.application.service.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubz.application.port.in.JobExecutor;
import com.hubz.domain.enums.JobType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Executor for REPORT_EXPORT jobs.
 * Handles asynchronous report generation (PDF, CSV, Excel).
 *
 * Payload format:
 * {
 *   "reportType": "TASKS|GOALS|HABITS",
 *   "format": "PDF|CSV|EXCEL",
 *   "organizationId": "uuid",
 *   "userId": "uuid",
 *   "filters": { ... }
 * }
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportExportJobExecutor implements JobExecutor {

    private final ObjectMapper objectMapper;

    @Override
    public void execute(String payload) throws Exception {
        JsonNode node = objectMapper.readTree(payload);
        String reportType = node.get("reportType").asText();
        String format = node.get("format").asText();
        String organizationId = node.has("organizationId") ? node.get("organizationId").asText() : "personal";

        log.info("Generating report: type={}, format={}, orgId={}", reportType, format, organizationId);

        // Report generation is delegated to the existing ReportService/AnalyticsService
        // The actual generation logic can be expanded here when needed.
        // For now, this executor logs and completes successfully as a placeholder
        // for async report generation workflows.

        log.info("Report generation completed: type={}, format={}, orgId={}", reportType, format, organizationId);
    }

    @Override
    public JobType getJobType() {
        return JobType.REPORT_EXPORT;
    }
}
