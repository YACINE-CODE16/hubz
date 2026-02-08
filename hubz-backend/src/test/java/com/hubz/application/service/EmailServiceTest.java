package com.hubz.application.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Unit Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@hubz.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:5175");
    }

    @Nested
    @DisplayName("Send Welcome Email Tests")
    class SendWelcomeEmailTests {

        @Test
        @DisplayName("Should send welcome email successfully")
        void shouldSendWelcomeEmailSuccessfully() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // When
            emailService.sendWelcomeEmail("user@example.com", "Jean");

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should not throw exception when email fails")
        void shouldNotThrowExceptionWhenEmailFails() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new RuntimeException("Email error")).when(mailSender).send(any(MimeMessage.class));

            // When - Should not throw
            emailService.sendWelcomeEmail("user@example.com", "Jean");

            // Then - No exception thrown
            verify(mailSender).createMimeMessage();
        }
    }

    @Nested
    @DisplayName("Send Weekly Digest Email Tests")
    class SendWeeklyDigestEmailTests {

        @Test
        @DisplayName("Should send weekly digest email successfully")
        void shouldSendWeeklyDigestEmailSuccessfully() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // When
            emailService.sendWeeklyDigestEmail(
                    "user@example.com",
                    "Jean",
                    10,  // tasksCompletedThisWeek
                    8,   // tasksCompletedLastWeek
                    3,   // goalsInProgress
                    2,   // goalsCompleted
                    85,  // habitsCompletionRate
                    5,   // upcomingEventsCount
                    "5 objectifs atteints cette semaine !"
            );

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should not throw exception when digest email fails")
        void shouldNotThrowExceptionWhenDigestEmailFails() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new RuntimeException("Email error")).when(mailSender).send(any(MimeMessage.class));

            // When - Should not throw
            emailService.sendWeeklyDigestEmail(
                    "user@example.com",
                    "Jean",
                    5, 3, 2, 1, 70, 3, null
            );

            // Then - No exception thrown
            verify(mailSender).createMimeMessage();
        }

        @Test
        @DisplayName("Should handle null achievement gracefully")
        void shouldHandleNullAchievement() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // When
            emailService.sendWeeklyDigestEmail(
                    "user@example.com",
                    "Jean",
                    0, 0, 0, 0, 0, 0, null
            );

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }
    }

    @Nested
    @DisplayName("Send Invitation Email Tests")
    class SendInvitationEmailTests {

        @Test
        @DisplayName("Should send invitation email successfully")
        void shouldSendInvitationEmailSuccessfully() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // When
            emailService.sendInvitationEmail(
                    "invitee@example.com",
                    "My Organization",
                    "abc123token",
                    "MEMBER"
            );

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }
    }

    @Nested
    @DisplayName("Send Password Reset Email Tests")
    class SendPasswordResetEmailTests {

        @Test
        @DisplayName("Should send password reset email successfully")
        void shouldSendPasswordResetEmailSuccessfully() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // When
            emailService.sendPasswordResetEmail(
                    "user@example.com",
                    "Jean",
                    "resettoken123"
            );

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }
    }

    @Nested
    @DisplayName("Send Email Verification Tests")
    class SendEmailVerificationTests {

        @Test
        @DisplayName("Should send email verification successfully")
        void shouldSendEmailVerificationSuccessfully() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // When
            emailService.sendEmailVerificationEmail(
                    "user@example.com",
                    "Jean",
                    "verifytoken123"
            );

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }
    }
}
