package com.hubz.application.service.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubz.application.service.EmailService;
import com.hubz.domain.enums.JobType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailJobExecutor Unit Tests")
class EmailJobExecutorTest {

    @Mock
    private EmailService emailService;

    private EmailJobExecutor emailJobExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        emailJobExecutor = new EmailJobExecutor(emailService, objectMapper);
    }

    @Test
    @DisplayName("Should return EMAIL_SEND as job type")
    void shouldReturnCorrectJobType() {
        assertThat(emailJobExecutor.getJobType()).isEqualTo(JobType.EMAIL_SEND);
    }

    @Test
    @DisplayName("Should execute invitation email job")
    void shouldExecuteInvitationEmail() throws Exception {
        // Arrange
        String payload = """
                {
                    "emailType": "INVITATION",
                    "to": "user@example.com",
                    "organizationName": "Test Org",
                    "token": "abc123",
                    "role": "MEMBER"
                }
                """;

        // Act
        emailJobExecutor.execute(payload);

        // Assert
        verify(emailService).sendInvitationEmail("user@example.com", "Test Org", "abc123", "MEMBER");
    }

    @Test
    @DisplayName("Should execute welcome email job")
    void shouldExecuteWelcomeEmail() throws Exception {
        // Arrange
        String payload = """
                {
                    "emailType": "WELCOME",
                    "to": "new@example.com",
                    "firstName": "Alice"
                }
                """;

        // Act
        emailJobExecutor.execute(payload);

        // Assert
        verify(emailService).sendWelcomeEmail("new@example.com", "Alice");
    }

    @Test
    @DisplayName("Should execute notification email job")
    void shouldExecuteNotificationEmail() throws Exception {
        // Arrange
        String payload = """
                {
                    "emailType": "NOTIFICATION",
                    "to": "user@example.com",
                    "firstName": "Bob",
                    "notificationType": "TASK_ASSIGNED",
                    "title": "New task assigned",
                    "message": "You have been assigned a task.",
                    "link": "/org/123/tasks"
                }
                """;

        // Act
        emailJobExecutor.execute(payload);

        // Assert
        verify(emailService).sendNotificationEmail(
                "user@example.com",
                "Bob",
                "TASK_ASSIGNED",
                "New task assigned",
                "You have been assigned a task.",
                "/org/123/tasks"
        );
    }

    @Test
    @DisplayName("Should throw exception for unknown email type")
    void shouldThrowForUnknownEmailType() {
        // Arrange
        String payload = """
                {
                    "emailType": "UNKNOWN_TYPE",
                    "to": "user@example.com"
                }
                """;

        // Act & Assert
        assertThatThrownBy(() -> emailJobExecutor.execute(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown email type: UNKNOWN_TYPE");
    }
}
