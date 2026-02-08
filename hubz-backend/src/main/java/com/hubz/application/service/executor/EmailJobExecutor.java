package com.hubz.application.service.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubz.application.port.in.JobExecutor;
import com.hubz.application.service.EmailService;
import com.hubz.domain.enums.JobType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Executor for EMAIL_SEND jobs.
 * Parses the JSON payload and delegates to EmailService for actual sending.
 *
 * Payload format:
 * {
 *   "emailType": "INVITATION|PASSWORD_RESET|VERIFICATION|WELCOME|NOTIFICATION|WEEKLY_DIGEST|DEADLINE_REMINDER",
 *   "to": "recipient@example.com",
 *   "firstName": "John",
 *   ... (additional fields depending on emailType)
 * }
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailJobExecutor implements JobExecutor {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(String payload) throws Exception {
        JsonNode node = objectMapper.readTree(payload);
        String emailType = node.get("emailType").asText();
        String to = node.get("to").asText();
        String firstName = node.has("firstName") ? node.get("firstName").asText() : "";

        switch (emailType) {
            case "INVITATION" -> {
                String organizationName = node.get("organizationName").asText();
                String token = node.get("token").asText();
                String role = node.get("role").asText();
                emailService.sendInvitationEmail(to, organizationName, token, role);
            }
            case "PASSWORD_RESET" -> {
                String token = node.get("token").asText();
                emailService.sendPasswordResetEmail(to, firstName, token);
            }
            case "VERIFICATION" -> {
                String token = node.get("token").asText();
                emailService.sendEmailVerificationEmail(to, firstName, token);
            }
            case "WELCOME" -> {
                emailService.sendWelcomeEmail(to, firstName);
            }
            case "NOTIFICATION" -> {
                String notificationType = node.get("notificationType").asText();
                String title = node.get("title").asText();
                String message = node.get("message").asText();
                String link = node.has("link") ? node.get("link").asText() : null;
                emailService.sendNotificationEmail(to, firstName, notificationType, title, message, link);
            }
            default -> {
                log.warn("Unknown email type: {}", emailType);
                throw new IllegalArgumentException("Unknown email type: " + emailType);
            }
        }

        log.info("Email job executed: type={}, to={}", emailType, to);
    }

    @Override
    public JobType getJobType() {
        return JobType.EMAIL_SEND;
    }
}
