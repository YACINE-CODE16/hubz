package com.hubz.application.service;

import com.hubz.application.port.out.UserPreferencesRepositoryPort;
import com.hubz.domain.model.UserPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyDigestScheduler Unit Tests")
class WeeklyDigestSchedulerTest {

    @Mock
    private UserPreferencesRepositoryPort preferencesRepository;

    @Mock
    private WeeklyDigestService weeklyDigestService;

    @InjectMocks
    private WeeklyDigestScheduler weeklyDigestScheduler;

    private UserPreferences userPrefs1;
    private UserPreferences userPrefs2;

    @BeforeEach
    void setUp() {
        userPrefs1 = UserPreferences.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .digestEnabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        userPrefs2 = UserPreferences.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .digestEnabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should send digest to all users with digest enabled")
    void shouldSendDigestToAllUsersWithDigestEnabled() {
        // Given
        when(preferencesRepository.findByDigestEnabledTrue())
                .thenReturn(List.of(userPrefs1, userPrefs2));

        // When
        weeklyDigestScheduler.sendWeeklyDigests();

        // Then
        verify(weeklyDigestService).sendWeeklyDigest(userPrefs1.getUserId());
        verify(weeklyDigestService).sendWeeklyDigest(userPrefs2.getUserId());
        verify(weeklyDigestService, times(2)).sendWeeklyDigest(any(UUID.class));
    }

    @Test
    @DisplayName("Should not send any digest when no users have digest enabled")
    void shouldNotSendDigestWhenNoUsersHaveDigestEnabled() {
        // Given
        when(preferencesRepository.findByDigestEnabledTrue())
                .thenReturn(Collections.emptyList());

        // When
        weeklyDigestScheduler.sendWeeklyDigests();

        // Then
        verify(weeklyDigestService, never()).sendWeeklyDigest(any(UUID.class));
    }

    @Test
    @DisplayName("Should continue processing other users when one fails")
    void shouldContinueProcessingWhenOneFails() {
        // Given
        when(preferencesRepository.findByDigestEnabledTrue())
                .thenReturn(List.of(userPrefs1, userPrefs2));
        doThrow(new RuntimeException("Failed")).when(weeklyDigestService)
                .sendWeeklyDigest(userPrefs1.getUserId());

        // When
        weeklyDigestScheduler.sendWeeklyDigests();

        // Then
        verify(weeklyDigestService).sendWeeklyDigest(userPrefs1.getUserId());
        verify(weeklyDigestService).sendWeeklyDigest(userPrefs2.getUserId());
    }

    @Test
    @DisplayName("Manual trigger should work the same as scheduled")
    void manualTriggerShouldWorkSameAsScheduled() {
        // Given
        when(preferencesRepository.findByDigestEnabledTrue())
                .thenReturn(List.of(userPrefs1));

        // When
        weeklyDigestScheduler.sendWeeklyDigestsManually();

        // Then
        verify(weeklyDigestService).sendWeeklyDigest(userPrefs1.getUserId());
    }
}
