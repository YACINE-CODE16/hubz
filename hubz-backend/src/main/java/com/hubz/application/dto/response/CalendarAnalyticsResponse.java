package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for calendar and time analytics.
 * Provides insights into event patterns, time distribution, and availability.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarAnalyticsResponse {

    // Overview metrics
    private long totalEvents;
    private long eventsInPeriod;
    private double totalHoursScheduled;
    private double averageEventDurationHours;

    // Events per period (for line/bar chart)
    private List<EventsPerPeriod> eventsPerWeek;
    private List<EventsPerPeriod> eventsPerMonth;

    // Time distribution by event type/category
    private Map<String, Double> timeDistribution;

    // Occupancy rate
    private double occupancyRate; // percentage of work hours occupied
    private List<DailyOccupancy> dailyOccupancy;

    // Busiest days heatmap data
    private Map<String, Double> busiestDaysOfWeek; // Day name -> average hours
    private List<DayHeatmapData> weeklyHeatmap;

    // Time slot distribution (hourly)
    private List<TimeSlotData> timeSlotDistribution;
    private String mostUsedTimeSlot; // e.g., "09:00-10:00"
    private String leastUsedTimeSlot;

    // Meeting vs work ratio
    private double meetingHours;
    private double personalEventHours;
    private double meetingVsPersonalRatio; // meetings / personal

    // Conflicts
    private int conflictCount;
    private List<AgendaConflict> conflicts;

    // Availability score (0-100)
    private int availabilityScore;
    private String availabilityInsight;

    // Upcoming week forecast
    private double forecastedHoursNextWeek;
    private int forecastedEventsNextWeek;

    // --- Nested DTOs ---

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventsPerPeriod {
        private String period; // "2026-W05" or "2026-01"
        private String label; // Human readable
        private long eventCount;
        private double totalHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyOccupancy {
        private String date;
        private double occupiedHours;
        private double availableHours;
        private double occupancyRate;
        private int eventCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayHeatmapData {
        private String dayOfWeek;
        private int dayIndex; // 0 = Monday, 6 = Sunday
        private double averageHours;
        private double averageEvents;
        private String intensity; // LOW, MEDIUM, HIGH, VERY_HIGH
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotData {
        private int hour; // 0-23
        private String timeSlot; // "09:00-10:00"
        private long eventCount;
        private double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgendaConflict {
        private String event1Id;
        private String event1Title;
        private String event2Id;
        private String event2Title;
        private String conflictDate;
        private String conflictTime;
        private int overlapMinutes;
    }
}
