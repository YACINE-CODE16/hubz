package com.hubz.application.service;

import com.hubz.application.dto.response.CalendarAnalyticsResponse;
import com.hubz.application.dto.response.CalendarAnalyticsResponse.*;
import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.domain.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarAnalyticsService Unit Tests")
class CalendarAnalyticsServiceTest {

    @Mock
    private EventRepositoryPort eventRepository;

    @InjectMocks
    private CalendarAnalyticsService calendarAnalyticsService;

    private UUID userId;
    private UUID orgId;
    private LocalDate today;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        today = LocalDate.now();
        startDate = today.minusDays(30);
        endDate = today;
    }

    @Nested
    @DisplayName("Get Calendar Analytics Tests")
    class GetCalendarAnalyticsTests {

        @Test
        @DisplayName("Should return empty analytics when user has no events")
        void shouldReturnEmptyAnalyticsWhenNoEvents() {
            // Given
            when(eventRepository.findAllByUserId(userId)).thenReturn(List.of());

            // When
            CalendarAnalyticsResponse response = calendarAnalyticsService.getCalendarAnalytics(
                    userId, startDate, endDate);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalEvents()).isZero();
            assertThat(response.getEventsInPeriod()).isZero();
            assertThat(response.getTotalHoursScheduled()).isZero();
            assertThat(response.getConflictCount()).isZero();
            assertThat(response.getAvailabilityScore()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should calculate correct total events count")
        void shouldCalculateCorrectTotalEvents() {
            // Given
            List<Event> events = List.of(
                    createEvent("Meeting 1", today.atTime(9, 0), today.atTime(10, 0), orgId),
                    createEvent("Meeting 2", today.atTime(14, 0), today.atTime(15, 0), orgId),
                    createEvent("Personal", today.atTime(18, 0), today.atTime(19, 0), null)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            CalendarAnalyticsResponse response = calendarAnalyticsService.getCalendarAnalytics(
                    userId, startDate, endDate);

            // Then
            assertThat(response.getTotalEvents()).isEqualTo(3);
            assertThat(response.getEventsInPeriod()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should calculate correct total hours scheduled")
        void shouldCalculateCorrectTotalHours() {
            // Given
            List<Event> events = List.of(
                    createEvent("2 Hour Meeting", today.atTime(9, 0), today.atTime(11, 0), orgId),
                    createEvent("1 Hour Meeting", today.atTime(14, 0), today.atTime(15, 0), orgId)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            CalendarAnalyticsResponse response = calendarAnalyticsService.getCalendarAnalytics(
                    userId, startDate, endDate);

            // Then
            assertThat(response.getTotalHoursScheduled()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("Should calculate average event duration correctly")
        void shouldCalculateAverageEventDuration() {
            // Given
            List<Event> events = List.of(
                    createEvent("1 Hour", today.atTime(9, 0), today.atTime(10, 0), orgId),
                    createEvent("2 Hours", today.atTime(14, 0), today.atTime(16, 0), orgId),
                    createEvent("3 Hours", today.atTime(18, 0), today.atTime(21, 0), null)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            CalendarAnalyticsResponse response = calendarAnalyticsService.getCalendarAnalytics(
                    userId, startDate, endDate);

            // Then
            // (1 + 2 + 3) / 3 = 2 hours average
            assertThat(response.getAverageEventDurationHours()).isEqualTo(2.0);
        }
    }

    @Nested
    @DisplayName("Time Distribution Tests")
    class TimeDistributionTests {

        @Test
        @DisplayName("Should calculate time distribution by event type")
        void shouldCalculateTimeDistribution() {
            // Given
            List<Event> events = List.of(
                    createEvent("Org Meeting", today.atTime(9, 0), today.atTime(11, 0), orgId), // 2h
                    createEvent("Personal Event", today.atTime(14, 0), today.atTime(15, 0), null) // 1h
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            Map<String, Double> distribution = calendarAnalyticsService.getTimeDistribution(
                    userId, startDate, endDate);

            // Then
            assertThat(distribution).containsKey("Organization Events");
            assertThat(distribution).containsKey("Personal Events");
            assertThat(distribution.get("Organization Events")).isEqualTo(2.0);
            assertThat(distribution.get("Personal Events")).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Occupancy Rate Tests")
    class OccupancyRateTests {

        @Test
        @DisplayName("Should calculate daily occupancy rate")
        void shouldCalculateDailyOccupancy() {
            // Given
            List<Event> events = List.of(
                    createEvent("4 Hour Meeting", today.atTime(9, 0), today.atTime(13, 0), orgId)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            List<DailyOccupancy> occupancy = calendarAnalyticsService.getOccupancyRate(
                    userId, today, today);

            // Then
            assertThat(occupancy).hasSize(1);
            DailyOccupancy todayOccupancy = occupancy.get(0);
            assertThat(todayOccupancy.getOccupiedHours()).isEqualTo(4.0);
            assertThat(todayOccupancy.getAvailableHours()).isEqualTo(8.0);
            assertThat(todayOccupancy.getOccupancyRate()).isEqualTo(50.0);
            assertThat(todayOccupancy.getEventCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should cap occupancy rate at 100%")
        void shouldCapOccupancyRateAt100() {
            // Given - 10 hours of events on a day with 8 work hours
            List<Event> events = List.of(
                    createEvent("Long Meeting", today.atTime(8, 0), today.atTime(18, 0), orgId)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            List<DailyOccupancy> occupancy = calendarAnalyticsService.getOccupancyRate(
                    userId, today, today);

            // Then
            assertThat(occupancy.get(0).getOccupancyRate()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("Busiest Days Tests")
    class BusiestDaysTests {

        @Test
        @DisplayName("Should build weekly heatmap with all days")
        void shouldBuildWeeklyHeatmap() {
            // Given
            when(eventRepository.findAllByUserId(userId)).thenReturn(List.of());

            // When
            List<DayHeatmapData> heatmap = calendarAnalyticsService.getBusiestDays(
                    userId, startDate, endDate);

            // Then
            assertThat(heatmap).hasSize(7);
            assertThat(heatmap.get(0).getDayOfWeek()).isEqualTo("MONDAY");
            assertThat(heatmap.get(6).getDayOfWeek()).isEqualTo("SUNDAY");
        }

        @Test
        @DisplayName("Should calculate intensity levels correctly")
        void shouldCalculateIntensityLevels() {
            // Given
            // Create a 6-hour meeting on Monday
            LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
            List<Event> events = List.of(
                    createEvent("Long Meeting", monday.atTime(9, 0), monday.atTime(15, 0), orgId)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            List<DayHeatmapData> heatmap = calendarAnalyticsService.getBusiestDays(
                    userId, monday, monday);

            // Then
            DayHeatmapData mondayData = heatmap.stream()
                    .filter(d -> d.getDayOfWeek().equals("MONDAY"))
                    .findFirst()
                    .orElseThrow();
            assertThat(mondayData.getAverageHours()).isEqualTo(6.0);
            assertThat(mondayData.getIntensity()).isEqualTo("VERY_HIGH");
        }
    }

    @Nested
    @DisplayName("Time Slot Distribution Tests")
    class TimeSlotDistributionTests {

        @Test
        @DisplayName("Should return distribution for all 24 hours")
        void shouldReturnDistributionForAll24Hours() {
            // Given
            when(eventRepository.findAllByUserId(userId)).thenReturn(List.of());

            // When
            List<TimeSlotData> distribution = calendarAnalyticsService.getMostUsedTimeSlots(userId);

            // Then
            assertThat(distribution).hasSize(24);
            assertThat(distribution.get(0).getHour()).isZero();
            assertThat(distribution.get(0).getTimeSlot()).isEqualTo("00:00-01:00");
            assertThat(distribution.get(23).getHour()).isEqualTo(23);
        }

        @Test
        @DisplayName("Should count events in correct time slots")
        void shouldCountEventsInCorrectTimeSlots() {
            // Given
            List<Event> events = List.of(
                    createEvent("9am Meeting", today.atTime(9, 0), today.atTime(10, 0), orgId),
                    createEvent("9am Meeting 2", today.minusDays(1).atTime(9, 0), today.minusDays(1).atTime(10, 0), orgId),
                    createEvent("2pm Meeting", today.atTime(14, 0), today.atTime(15, 0), orgId)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            List<TimeSlotData> distribution = calendarAnalyticsService.getMostUsedTimeSlots(userId);

            // Then
            TimeSlotData nineAmSlot = distribution.stream()
                    .filter(d -> d.getHour() == 9)
                    .findFirst()
                    .orElseThrow();
            assertThat(nineAmSlot.getEventCount()).isEqualTo(2);

            TimeSlotData twoPmSlot = distribution.stream()
                    .filter(d -> d.getHour() == 14)
                    .findFirst()
                    .orElseThrow();
            assertThat(twoPmSlot.getEventCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should identify most used time slot")
        void shouldIdentifyMostUsedTimeSlot() {
            // Given
            List<Event> events = List.of(
                    createEvent("Meeting 1", today.atTime(10, 0), today.atTime(11, 0), orgId),
                    createEvent("Meeting 2", today.minusDays(1).atTime(10, 0), today.minusDays(1).atTime(11, 0), orgId),
                    createEvent("Meeting 3", today.minusDays(2).atTime(10, 0), today.minusDays(2).atTime(11, 0), orgId)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            CalendarAnalyticsResponse response = calendarAnalyticsService.getCalendarAnalytics(
                    userId, startDate, endDate);

            // Then
            assertThat(response.getMostUsedTimeSlot()).isEqualTo("10:00-11:00");
        }
    }

    @Nested
    @DisplayName("Meeting vs Work Ratio Tests")
    class MeetingVsWorkRatioTests {

        @Test
        @DisplayName("Should calculate meeting vs personal hours correctly")
        void shouldCalculateMeetingVsPersonalHours() {
            // Given
            List<Event> events = List.of(
                    createEvent("Org Meeting", today.atTime(9, 0), today.atTime(11, 0), orgId), // 2h meeting
                    createEvent("Personal", today.atTime(14, 0), today.atTime(15, 0), null) // 1h personal
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            Map<String, Double> ratio = calendarAnalyticsService.getMeetingVsWorkRatio(
                    userId, startDate, endDate);

            // Then
            assertThat(ratio.get("meetingHours")).isEqualTo(2.0);
            assertThat(ratio.get("personalHours")).isEqualTo(1.0);
            assertThat(ratio.get("totalHours")).isEqualTo(3.0);
            assertThat(ratio.get("meetingPercentage")).isCloseTo(66.67, org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        @DisplayName("Should handle zero personal hours without division error")
        void shouldHandleZeroPersonalHours() {
            // Given
            List<Event> events = List.of(
                    createEvent("Meeting Only", today.atTime(9, 0), today.atTime(10, 0), orgId)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            CalendarAnalyticsResponse response = calendarAnalyticsService.getCalendarAnalytics(
                    userId, startDate, endDate);

            // Then
            assertThat(response.getMeetingHours()).isEqualTo(1.0);
            assertThat(response.getPersonalEventHours()).isZero();
            // Ratio should be 0 when personal is 0 (avoiding division by zero)
            assertThat(response.getMeetingVsPersonalRatio()).isZero();
        }
    }

    @Nested
    @DisplayName("Conflict Detection Tests")
    class ConflictDetectionTests {

        @Test
        @DisplayName("Should detect overlapping events as conflicts")
        void shouldDetectOverlappingEvents() {
            // Given
            List<Event> events = List.of(
                    createEventWithId(UUID.randomUUID(), "Meeting 1", today.atTime(9, 0), today.atTime(11, 0), orgId),
                    createEventWithId(UUID.randomUUID(), "Meeting 2", today.atTime(10, 0), today.atTime(12, 0), orgId)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            List<AgendaConflict> conflicts = calendarAnalyticsService.getAgendaConflicts(
                    userId, startDate, endDate);

            // Then
            assertThat(conflicts).hasSize(1);
            assertThat(conflicts.get(0).getOverlapMinutes()).isEqualTo(60);
        }

        @Test
        @DisplayName("Should not detect non-overlapping events as conflicts")
        void shouldNotDetectNonOverlappingEvents() {
            // Given
            List<Event> events = List.of(
                    createEvent("Meeting 1", today.atTime(9, 0), today.atTime(10, 0), orgId),
                    createEvent("Meeting 2", today.atTime(10, 0), today.atTime(11, 0), orgId)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            List<AgendaConflict> conflicts = calendarAnalyticsService.getAgendaConflicts(
                    userId, startDate, endDate);

            // Then
            assertThat(conflicts).isEmpty();
        }

        @Test
        @DisplayName("Should detect multiple conflicts")
        void shouldDetectMultipleConflicts() {
            // Given
            List<Event> events = List.of(
                    createEventWithId(UUID.randomUUID(), "Event 1", today.atTime(9, 0), today.atTime(12, 0), orgId),
                    createEventWithId(UUID.randomUUID(), "Event 2", today.atTime(10, 0), today.atTime(11, 0), orgId),
                    createEventWithId(UUID.randomUUID(), "Event 3", today.atTime(11, 0), today.atTime(13, 0), orgId)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            List<AgendaConflict> conflicts = calendarAnalyticsService.getAgendaConflicts(
                    userId, startDate, endDate);

            // Then
            assertThat(conflicts).hasSize(2); // Event1-Event2 and Event1-Event3
        }
    }

    @Nested
    @DisplayName("Availability Score Tests")
    class AvailabilityScoreTests {

        @Test
        @DisplayName("Should return high score for light schedule")
        void shouldReturnHighScoreForLightSchedule() {
            // Given
            List<Event> events = List.of(
                    createEvent("Single Meeting", today.plusDays(1).atTime(10, 0), today.plusDays(1).atTime(11, 0), orgId)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            int score = calendarAnalyticsService.getAvailabilityScore(userId);

            // Then
            assertThat(score).isGreaterThanOrEqualTo(70);
        }

        @Test
        @DisplayName("Should return lower score for heavy schedule with conflicts")
        void shouldReturnLowerScoreForHeavyScheduleWithConflicts() {
            // Given - Many overlapping events
            List<Event> events = List.of(
                    createEventWithId(UUID.randomUUID(), "Event 1", today.plusDays(1).atTime(8, 0), today.plusDays(1).atTime(18, 0), orgId),
                    createEventWithId(UUID.randomUUID(), "Event 2", today.plusDays(1).atTime(9, 0), today.plusDays(1).atTime(12, 0), orgId),
                    createEventWithId(UUID.randomUUID(), "Event 3", today.plusDays(2).atTime(8, 0), today.plusDays(2).atTime(18, 0), orgId),
                    createEventWithId(UUID.randomUUID(), "Event 4", today.plusDays(3).atTime(8, 0), today.plusDays(3).atTime(18, 0), orgId),
                    createEventWithId(UUID.randomUUID(), "Event 5", today.plusDays(4).atTime(8, 0), today.plusDays(4).atTime(18, 0), orgId)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            int score = calendarAnalyticsService.getAvailabilityScore(userId);

            // Then
            assertThat(score).isLessThan(80);
        }

        @Test
        @DisplayName("Should generate appropriate availability insight")
        void shouldGenerateAppropriateInsight() {
            // Given
            when(eventRepository.findAllByUserId(userId)).thenReturn(List.of());

            // When
            CalendarAnalyticsResponse response = calendarAnalyticsService.getCalendarAnalytics(
                    userId, startDate, endDate);

            // Then
            assertThat(response.getAvailabilityInsight()).isNotBlank();
            assertThat(response.getAvailabilityScore()).isGreaterThanOrEqualTo(80);
            assertThat(response.getAvailabilityInsight()).contains("Excellente");
        }
    }

    @Nested
    @DisplayName("Events Per Period Tests")
    class EventsPerPeriodTests {

        @Test
        @DisplayName("Should group events by week")
        void shouldGroupEventsByWeek() {
            // Given
            List<Event> events = List.of(
                    createEvent("Event 1", today.atTime(10, 0), today.atTime(11, 0), orgId),
                    createEvent("Event 2", today.minusWeeks(1).atTime(10, 0), today.minusWeeks(1).atTime(11, 0), orgId)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            List<EventsPerPeriod> eventsPerWeek = calendarAnalyticsService.getEventsPerPeriod(
                    userId, startDate, endDate, "WEEK");

            // Then
            assertThat(eventsPerWeek).isNotEmpty();
            // Verify structure
            assertThat(eventsPerWeek.get(0).getPeriod()).matches("\\d{4}-W\\d{2}");
            assertThat(eventsPerWeek.get(0).getLabel()).startsWith("Semaine");
        }

        @Test
        @DisplayName("Should group events by month")
        void shouldGroupEventsByMonth() {
            // Given
            List<Event> events = List.of(
                    createEvent("Event 1", today.atTime(10, 0), today.atTime(11, 0), orgId)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            List<EventsPerPeriod> eventsPerMonth = calendarAnalyticsService.getEventsPerPeriod(
                    userId, startDate, endDate, "MONTH");

            // Then
            assertThat(eventsPerMonth).isNotEmpty();
            // Verify structure
            assertThat(eventsPerMonth.get(0).getPeriod()).matches("\\d{4}-\\d{2}");
        }
    }

    @Nested
    @DisplayName("Forecast Tests")
    class ForecastTests {

        @Test
        @DisplayName("Should forecast next week events")
        void shouldForecastNextWeekEvents() {
            // Given
            List<Event> events = List.of(
                    createEvent("Next Week Event", today.plusDays(2).atTime(10, 0), today.plusDays(2).atTime(12, 0), orgId),
                    createEvent("Another Next Week", today.plusDays(3).atTime(14, 0), today.plusDays(3).atTime(15, 0), orgId)
            );
            when(eventRepository.findAllByUserId(userId)).thenReturn(events);

            // When
            CalendarAnalyticsResponse response = calendarAnalyticsService.getCalendarAnalytics(
                    userId, startDate, endDate);

            // Then
            assertThat(response.getForecastedEventsNextWeek()).isEqualTo(2);
            assertThat(response.getForecastedHoursNextWeek()).isEqualTo(3.0); // 2h + 1h
        }
    }

    // Helper methods

    private Event createEvent(String title, LocalDateTime start, LocalDateTime end, UUID organizationId) {
        return Event.builder()
                .id(UUID.randomUUID())
                .title(title)
                .startTime(start)
                .endTime(end)
                .organizationId(organizationId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Event createEventWithId(UUID id, String title, LocalDateTime start, LocalDateTime end, UUID organizationId) {
        return Event.builder()
                .id(id)
                .title(title)
                .startTime(start)
                .endTime(end)
                .organizationId(organizationId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
