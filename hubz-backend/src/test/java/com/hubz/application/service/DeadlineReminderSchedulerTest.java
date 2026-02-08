package com.hubz.application.service;

import com.hubz.application.port.out.UserPreferencesRepositoryPort;
import com.hubz.domain.enums.ReminderFrequency;
import com.hubz.domain.model.UserPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadlineReminderSchedulerTest {

    @Mock
    private UserPreferencesRepositoryPort preferencesRepository;

    @Mock
    private DeadlineReminderService deadlineReminderService;

    @InjectMocks
    private DeadlineReminderScheduler scheduler;

    private UUID userId1;
    private UUID userId2;
    private UserPreferences prefs1;
    private UserPreferences prefs2;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();

        prefs1 = UserPreferences.builder()
                .id(UUID.randomUUID())
                .userId(userId1)
                .reminderEnabled(true)
                .reminderFrequency(ReminderFrequency.THREE_DAYS)
                .build();

        prefs2 = UserPreferences.builder()
                .id(UUID.randomUUID())
                .userId(userId2)
                .reminderEnabled(true)
                .reminderFrequency(ReminderFrequency.ONE_WEEK)
                .build();
    }

    @Test
    @DisplayName("should do nothing when no users have reminders enabled")
    void shouldDoNothingWhenNoUsersHaveRemindersEnabled() {
        // Given
        when(preferencesRepository.findByReminderEnabledTrue()).thenReturn(Collections.emptyList());

        // When
        scheduler.sendDeadlineReminders();

        // Then
        verify(deadlineReminderService, never()).generateReminders(any(), any());
        verify(deadlineReminderService, never()).sendDeadlineReminder(any(), any());
    }

    @Test
    @DisplayName("should process all users with reminders enabled")
    void shouldProcessAllUsersWithRemindersEnabled() {
        // Given
        when(preferencesRepository.findByReminderEnabledTrue()).thenReturn(List.of(prefs1, prefs2));

        DeadlineReminderService.DeadlineReminderData emptyData =
                new DeadlineReminderService.DeadlineReminderData(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()
                );
        when(deadlineReminderService.generateReminders(any(), any())).thenReturn(emptyData);

        // When
        scheduler.sendDeadlineReminders();

        // Then
        verify(deadlineReminderService).generateReminders(eq(userId1), eq(prefs1));
        verify(deadlineReminderService).generateReminders(eq(userId2), eq(prefs2));
    }

    @Test
    @DisplayName("should send reminders only when user has items")
    void shouldSendRemindersOnlyWhenUserHasItems() {
        // Given
        when(preferencesRepository.findByReminderEnabledTrue()).thenReturn(List.of(prefs1, prefs2));

        DeadlineReminderService.DeadlineItem item = new DeadlineReminderService.DeadlineItem(
                "Tache", "Test Task", java.time.LocalDate.now(), "Demain", UUID.randomUUID(), null
        );
        DeadlineReminderService.DeadlineReminderData dataWithItems =
                new DeadlineReminderService.DeadlineReminderData(
                        List.of(item),
                        Collections.emptyList(),
                        Collections.emptyList()
                );
        DeadlineReminderService.DeadlineReminderData emptyData =
                new DeadlineReminderService.DeadlineReminderData(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()
                );

        // User 1 has items, user 2 does not
        when(deadlineReminderService.generateReminders(eq(userId1), eq(prefs1))).thenReturn(dataWithItems);
        when(deadlineReminderService.generateReminders(eq(userId2), eq(prefs2))).thenReturn(emptyData);

        // When
        scheduler.sendDeadlineReminders();

        // Then
        verify(deadlineReminderService).sendDeadlineReminder(eq(userId1), eq(prefs1));
        verify(deadlineReminderService, never()).sendDeadlineReminder(eq(userId2), any());
    }

    @Test
    @DisplayName("should continue processing other users when one fails")
    void shouldContinueProcessingWhenOneFails() {
        // Given
        when(preferencesRepository.findByReminderEnabledTrue()).thenReturn(List.of(prefs1, prefs2));

        DeadlineReminderService.DeadlineItem item = new DeadlineReminderService.DeadlineItem(
                "Tache", "Test Task", java.time.LocalDate.now(), "Demain", UUID.randomUUID(), null
        );
        DeadlineReminderService.DeadlineReminderData dataWithItems =
                new DeadlineReminderService.DeadlineReminderData(
                        List.of(item),
                        Collections.emptyList(),
                        Collections.emptyList()
                );

        // First user throws exception
        when(deadlineReminderService.generateReminders(eq(userId1), eq(prefs1)))
                .thenThrow(new RuntimeException("Test error"));
        // Second user succeeds
        when(deadlineReminderService.generateReminders(eq(userId2), eq(prefs2))).thenReturn(dataWithItems);

        // When
        scheduler.sendDeadlineReminders();

        // Then - second user should still be processed
        verify(deadlineReminderService).generateReminders(eq(userId2), eq(prefs2));
        verify(deadlineReminderService).sendDeadlineReminder(eq(userId2), eq(prefs2));
    }

    @Test
    @DisplayName("manual trigger should call the main method")
    void manualTriggerShouldCallMainMethod() {
        // Given
        when(preferencesRepository.findByReminderEnabledTrue()).thenReturn(Collections.emptyList());

        // When
        scheduler.sendDeadlineRemindersManually();

        // Then
        verify(preferencesRepository).findByReminderEnabledTrue();
    }
}
