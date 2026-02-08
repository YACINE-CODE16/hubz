package com.hubz.application.service;

import com.hubz.application.dto.request.UpdatePreferencesRequest;
import com.hubz.application.dto.response.UserPreferencesResponse;
import com.hubz.application.port.out.UserPreferencesRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.DateFormat;
import com.hubz.domain.enums.Language;
import com.hubz.domain.enums.Theme;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.User;
import com.hubz.domain.model.UserPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPreferencesService Unit Tests")
class UserPreferencesServiceTest {

    @Mock
    private UserPreferencesRepositoryPort preferencesRepositoryPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @InjectMocks
    private UserPreferencesService preferencesService;

    private User testUser;
    private UserPreferences testPreferences;
    private final String userEmail = "test@example.com";
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(userId)
                .email(userEmail)
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();

        testPreferences = UserPreferences.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .language(Language.FR)
                .timezone("Europe/Paris")
                .dateFormat(DateFormat.DD_MM_YYYY)
                .theme(Theme.SYSTEM)
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();
    }

    @Nested
    @DisplayName("Get Preferences Tests")
    class GetPreferencesTests {

        @Test
        @DisplayName("Should return existing preferences for user")
        void shouldReturnExistingPreferences() {
            // Given
            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));

            // When
            UserPreferencesResponse response = preferencesService.getPreferences(userEmail);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getLanguage()).isEqualTo(Language.FR);
            assertThat(response.getTimezone()).isEqualTo("Europe/Paris");
            assertThat(response.getDateFormat()).isEqualTo(DateFormat.DD_MM_YYYY);
            assertThat(response.getTheme()).isEqualTo(Theme.SYSTEM);

            verify(userRepositoryPort).findByEmail(userEmail);
            verify(preferencesRepositoryPort).findByUserId(userId);
        }

        @Test
        @DisplayName("Should create default preferences if none exist")
        void shouldCreateDefaultPreferencesIfNoneExist() {
            // Given
            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.empty());
            when(preferencesRepositoryPort.save(any(UserPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserPreferencesResponse response = preferencesService.getPreferences(userEmail);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getLanguage()).isEqualTo(Language.FR);
            assertThat(response.getTimezone()).isEqualTo("Europe/Paris");
            assertThat(response.getDateFormat()).isEqualTo(DateFormat.DD_MM_YYYY);
            assertThat(response.getTheme()).isEqualTo(Theme.SYSTEM);

            verify(preferencesRepositoryPort).save(any(UserPreferences.class));
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            String unknownEmail = "unknown@example.com";
            when(userRepositoryPort.findByEmail(unknownEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> preferencesService.getPreferences(unknownEmail))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepositoryPort).findByEmail(unknownEmail);
            verify(preferencesRepositoryPort, never()).findByUserId(any());
        }

        @Test
        @DisplayName("Should return preferences by user ID")
        void shouldReturnPreferencesByUserId() {
            // Given
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));

            // When
            UserPreferencesResponse response = preferencesService.getPreferencesByUserId(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getLanguage()).isEqualTo(Language.FR);

            verify(preferencesRepositoryPort).findByUserId(userId);
        }

        @Test
        @DisplayName("Should create default preferences when getting by user ID if none exist")
        void shouldCreateDefaultPreferencesForUserId() {
            // Given
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.empty());
            when(preferencesRepositoryPort.save(any(UserPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserPreferencesResponse response = preferencesService.getPreferencesByUserId(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);

            verify(preferencesRepositoryPort).save(any(UserPreferences.class));
        }
    }

    @Nested
    @DisplayName("Update Preferences Tests")
    class UpdatePreferencesTests {

        @Test
        @DisplayName("Should successfully update all preferences")
        void shouldUpdateAllPreferences() {
            // Given
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .language(Language.EN)
                    .timezone("America/New_York")
                    .dateFormat(DateFormat.MM_DD_YYYY)
                    .theme(Theme.DARK)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));
            when(preferencesRepositoryPort.save(any(UserPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserPreferencesResponse response = preferencesService.updatePreferences(userEmail, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getLanguage()).isEqualTo(Language.EN);
            assertThat(response.getTimezone()).isEqualTo("America/New_York");
            assertThat(response.getDateFormat()).isEqualTo(DateFormat.MM_DD_YYYY);
            assertThat(response.getTheme()).isEqualTo(Theme.DARK);

            verify(preferencesRepositoryPort).save(any(UserPreferences.class));
        }

        @Test
        @DisplayName("Should update language only")
        void shouldUpdateLanguageOnly() {
            // Given
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .language(Language.EN)
                    .timezone("Europe/Paris")
                    .dateFormat(DateFormat.DD_MM_YYYY)
                    .theme(Theme.SYSTEM)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));
            when(preferencesRepositoryPort.save(any(UserPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserPreferencesResponse response = preferencesService.updatePreferences(userEmail, request);

            // Then
            assertThat(response.getLanguage()).isEqualTo(Language.EN);
            assertThat(response.getTimezone()).isEqualTo("Europe/Paris");
            assertThat(response.getDateFormat()).isEqualTo(DateFormat.DD_MM_YYYY);
            assertThat(response.getTheme()).isEqualTo(Theme.SYSTEM);
        }

        @Test
        @DisplayName("Should update theme to light mode")
        void shouldUpdateThemeToLight() {
            // Given
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .language(Language.FR)
                    .timezone("Europe/Paris")
                    .dateFormat(DateFormat.DD_MM_YYYY)
                    .theme(Theme.LIGHT)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));
            when(preferencesRepositoryPort.save(any(UserPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserPreferencesResponse response = preferencesService.updatePreferences(userEmail, request);

            // Then
            assertThat(response.getTheme()).isEqualTo(Theme.LIGHT);
        }

        @Test
        @DisplayName("Should update the updatedAt timestamp")
        void shouldUpdateTimestamp() {
            // Given
            LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .language(Language.EN)
                    .timezone("Europe/London")
                    .dateFormat(DateFormat.DD_MM_YYYY)
                    .theme(Theme.DARK)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));
            when(preferencesRepositoryPort.save(any(UserPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ArgumentCaptor<UserPreferences> prefsCaptor = ArgumentCaptor.forClass(UserPreferences.class);

            // When
            preferencesService.updatePreferences(userEmail, request);

            // Then
            verify(preferencesRepositoryPort).save(prefsCaptor.capture());
            UserPreferences savedPrefs = prefsCaptor.getValue();
            assertThat(savedPrefs.getUpdatedAt()).isAfter(beforeUpdate);
        }

        @Test
        @DisplayName("Should create preferences if none exist when updating")
        void shouldCreatePreferencesIfNoneExistWhenUpdating() {
            // Given
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .language(Language.EN)
                    .timezone("America/New_York")
                    .dateFormat(DateFormat.MM_DD_YYYY)
                    .theme(Theme.DARK)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.empty());
            when(preferencesRepositoryPort.save(any(UserPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserPreferencesResponse response = preferencesService.updatePreferences(userEmail, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getLanguage()).isEqualTo(Language.EN);

            // Should be called twice: once for default creation, once for update
            verify(preferencesRepositoryPort, times(2)).save(any(UserPreferences.class));
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found for update")
        void shouldThrowExceptionWhenUserNotFoundForUpdate() {
            // Given
            String unknownEmail = "unknown@example.com";
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .language(Language.EN)
                    .timezone("Europe/Paris")
                    .dateFormat(DateFormat.DD_MM_YYYY)
                    .theme(Theme.SYSTEM)
                    .build();

            when(userRepositoryPort.findByEmail(unknownEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> preferencesService.updatePreferences(unknownEmail, request))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepositoryPort).findByEmail(unknownEmail);
            verify(preferencesRepositoryPort, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception for invalid timezone")
        void shouldThrowExceptionForInvalidTimezone() {
            // Given
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .language(Language.EN)
                    .timezone("Invalid/Timezone")
                    .dateFormat(DateFormat.DD_MM_YYYY)
                    .theme(Theme.SYSTEM)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> preferencesService.updatePreferences(userEmail, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid timezone");

            verify(preferencesRepositoryPort, never()).save(any());
        }

        @Test
        @DisplayName("Should accept valid timezone Europe/London")
        void shouldAcceptValidTimezoneEuropeLondon() {
            // Given
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .language(Language.EN)
                    .timezone("Europe/London")
                    .dateFormat(DateFormat.DD_MM_YYYY)
                    .theme(Theme.SYSTEM)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));
            when(preferencesRepositoryPort.save(any(UserPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserPreferencesResponse response = preferencesService.updatePreferences(userEmail, request);

            // Then
            assertThat(response.getTimezone()).isEqualTo("Europe/London");
        }

        @Test
        @DisplayName("Should accept valid timezone Asia/Tokyo")
        void shouldAcceptValidTimezoneAsiaTokyo() {
            // Given
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .language(Language.EN)
                    .timezone("Asia/Tokyo")
                    .dateFormat(DateFormat.YYYY_MM_DD)
                    .theme(Theme.DARK)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));
            when(preferencesRepositoryPort.save(any(UserPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserPreferencesResponse response = preferencesService.updatePreferences(userEmail, request);

            // Then
            assertThat(response.getTimezone()).isEqualTo("Asia/Tokyo");
        }

        @Test
        @DisplayName("Should accept UTC timezone")
        void shouldAcceptUTCTimezone() {
            // Given
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .language(Language.FR)
                    .timezone("UTC")
                    .dateFormat(DateFormat.DD_MM_YYYY)
                    .theme(Theme.SYSTEM)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));
            when(preferencesRepositoryPort.save(any(UserPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserPreferencesResponse response = preferencesService.updatePreferences(userEmail, request);

            // Then
            assertThat(response.getTimezone()).isEqualTo("UTC");
        }
    }

    @Nested
    @DisplayName("Supported Timezones Tests")
    class SupportedTimezonesTests {

        @Test
        @DisplayName("Should return non-empty set of timezones")
        void shouldReturnNonEmptySetOfTimezones() {
            // When
            Set<String> timezones = preferencesService.getSupportedTimezones();

            // Then
            assertThat(timezones).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("Should include common European timezones")
        void shouldIncludeEuropeanTimezones() {
            // When
            Set<String> timezones = preferencesService.getSupportedTimezones();

            // Then
            assertThat(timezones).contains("Europe/Paris", "Europe/London", "Europe/Berlin");
        }

        @Test
        @DisplayName("Should include common American timezones")
        void shouldIncludeAmericanTimezones() {
            // When
            Set<String> timezones = preferencesService.getSupportedTimezones();

            // Then
            assertThat(timezones).contains("America/New_York", "America/Los_Angeles", "America/Chicago");
        }

        @Test
        @DisplayName("Should include UTC")
        void shouldIncludeUTC() {
            // When
            Set<String> timezones = preferencesService.getSupportedTimezones();

            // Then
            assertThat(timezones).contains("UTC");
        }

        @Test
        @DisplayName("Should include Asian timezones")
        void shouldIncludeAsianTimezones() {
            // When
            Set<String> timezones = preferencesService.getSupportedTimezones();

            // Then
            assertThat(timezones).contains("Asia/Tokyo", "Asia/Shanghai", "Asia/Singapore");
        }
    }

    @Nested
    @DisplayName("Date Format Tests")
    class DateFormatTests {

        @Test
        @DisplayName("Should support DD/MM/YYYY format")
        void shouldSupportDDMMYYYYFormat() {
            // Given
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .language(Language.FR)
                    .timezone("Europe/Paris")
                    .dateFormat(DateFormat.DD_MM_YYYY)
                    .theme(Theme.SYSTEM)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));
            when(preferencesRepositoryPort.save(any(UserPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserPreferencesResponse response = preferencesService.updatePreferences(userEmail, request);

            // Then
            assertThat(response.getDateFormat()).isEqualTo(DateFormat.DD_MM_YYYY);
            assertThat(response.getDateFormat().getPattern()).isEqualTo("DD/MM/YYYY");
        }

        @Test
        @DisplayName("Should support MM/DD/YYYY format")
        void shouldSupportMMDDYYYYFormat() {
            // Given
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .language(Language.EN)
                    .timezone("America/New_York")
                    .dateFormat(DateFormat.MM_DD_YYYY)
                    .theme(Theme.SYSTEM)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));
            when(preferencesRepositoryPort.save(any(UserPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserPreferencesResponse response = preferencesService.updatePreferences(userEmail, request);

            // Then
            assertThat(response.getDateFormat()).isEqualTo(DateFormat.MM_DD_YYYY);
            assertThat(response.getDateFormat().getPattern()).isEqualTo("MM/DD/YYYY");
        }

        @Test
        @DisplayName("Should support YYYY-MM-DD format")
        void shouldSupportYYYYMMDDFormat() {
            // Given
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .language(Language.EN)
                    .timezone("Asia/Tokyo")
                    .dateFormat(DateFormat.YYYY_MM_DD)
                    .theme(Theme.DARK)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));
            when(preferencesRepositoryPort.save(any(UserPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserPreferencesResponse response = preferencesService.updatePreferences(userEmail, request);

            // Then
            assertThat(response.getDateFormat()).isEqualTo(DateFormat.YYYY_MM_DD);
            assertThat(response.getDateFormat().getPattern()).isEqualTo("YYYY-MM-DD");
        }
    }
}
