package com.hubz.application.service;

import com.hubz.application.dto.response.CalendarAnalyticsResponse;
import com.hubz.application.dto.response.CalendarAnalyticsResponse.*;
import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.domain.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calendar and time analytics.
 * Provides insights into event patterns, time distribution, and availability.
 */
@Service
@RequiredArgsConstructor
public class CalendarAnalyticsService {

    private final EventRepositoryPort eventRepository;

    private static final int WORK_HOURS_PER_DAY = 8;
    private static final int WORK_START_HOUR = 9;
    private static final int WORK_END_HOUR = 18;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Get comprehensive calendar analytics for a user.
     *
     * @param userId    The user ID
     * @param startDate Start of the analysis period
     * @param endDate   End of the analysis period
     * @return Calendar analytics response
     */
    public CalendarAnalyticsResponse getCalendarAnalytics(UUID userId, LocalDate startDate, LocalDate endDate) {
        // Fetch all events for the user within the period
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Event> allUserEvents = eventRepository.findAllByUserId(userId);
        List<Event> eventsInPeriod = allUserEvents.stream()
                .filter(e -> !e.getEndTime().isBefore(startDateTime) && !e.getStartTime().isAfter(endDateTime))
                .toList();

        // Calculate overview metrics
        long totalEvents = allUserEvents.size();
        long eventsInPeriodCount = eventsInPeriod.size();
        double totalHoursScheduled = calculateTotalHours(eventsInPeriod);
        double avgDuration = eventsInPeriodCount > 0 ? totalHoursScheduled / eventsInPeriodCount : 0;

        // Events per week
        List<EventsPerPeriod> eventsPerWeek = calculateEventsPerWeek(eventsInPeriod, startDate, endDate);

        // Events per month
        List<EventsPerPeriod> eventsPerMonth = calculateEventsPerMonth(eventsInPeriod, startDate, endDate);

        // Time distribution by type (personal vs org events)
        Map<String, Double> timeDistribution = calculateTimeDistribution(eventsInPeriod);

        // Occupancy rate
        double occupancyRate = calculateOccupancyRate(eventsInPeriod, startDate, endDate);
        List<DailyOccupancy> dailyOccupancy = calculateDailyOccupancy(eventsInPeriod, startDate, endDate);

        // Busiest days
        Map<String, Double> busiestDaysOfWeek = calculateBusiestDays(eventsInPeriod);
        List<DayHeatmapData> weeklyHeatmap = buildWeeklyHeatmap(eventsInPeriod);

        // Time slot distribution
        List<TimeSlotData> timeSlotDistribution = calculateTimeSlotDistribution(eventsInPeriod);
        String mostUsedSlot = findMostUsedTimeSlot(timeSlotDistribution);
        String leastUsedSlot = findLeastUsedTimeSlot(timeSlotDistribution);

        // Meeting vs work ratio (org events vs personal)
        double meetingHours = calculateMeetingHours(eventsInPeriod);
        double personalHours = totalHoursScheduled - meetingHours;
        double ratio = personalHours > 0 ? meetingHours / personalHours : 0;

        // Conflicts detection
        List<AgendaConflict> conflicts = detectConflicts(eventsInPeriod);
        int conflictCount = conflicts.size();

        // Availability score
        int availabilityScore = calculateAvailabilityScore(occupancyRate, conflictCount, eventsInPeriodCount);
        String availabilityInsight = generateAvailabilityInsight(availabilityScore, occupancyRate);

        // Forecast next week
        LocalDate nextWeekStart = LocalDate.now().plusDays(1);
        LocalDate nextWeekEnd = nextWeekStart.plusDays(6);
        List<Event> nextWeekEvents = allUserEvents.stream()
                .filter(e -> !e.getEndTime().toLocalDate().isBefore(nextWeekStart)
                        && !e.getStartTime().toLocalDate().isAfter(nextWeekEnd))
                .toList();
        double forecastedHours = calculateTotalHours(nextWeekEvents);
        int forecastedCount = nextWeekEvents.size();

        return CalendarAnalyticsResponse.builder()
                .totalEvents(totalEvents)
                .eventsInPeriod(eventsInPeriodCount)
                .totalHoursScheduled(round(totalHoursScheduled))
                .averageEventDurationHours(round(avgDuration))
                .eventsPerWeek(eventsPerWeek)
                .eventsPerMonth(eventsPerMonth)
                .timeDistribution(timeDistribution)
                .occupancyRate(round(occupancyRate))
                .dailyOccupancy(dailyOccupancy)
                .busiestDaysOfWeek(busiestDaysOfWeek)
                .weeklyHeatmap(weeklyHeatmap)
                .timeSlotDistribution(timeSlotDistribution)
                .mostUsedTimeSlot(mostUsedSlot)
                .leastUsedTimeSlot(leastUsedSlot)
                .meetingHours(round(meetingHours))
                .personalEventHours(round(personalHours))
                .meetingVsPersonalRatio(round(ratio))
                .conflictCount(conflictCount)
                .conflicts(conflicts)
                .availabilityScore(availabilityScore)
                .availabilityInsight(availabilityInsight)
                .forecastedHoursNextWeek(round(forecastedHours))
                .forecastedEventsNextWeek(forecastedCount)
                .build();
    }

    /**
     * Get events per period with custom grouping.
     */
    public List<EventsPerPeriod> getEventsPerPeriod(UUID userId, LocalDate startDate, LocalDate endDate, String groupBy) {
        List<Event> events = getEventsInRange(userId, startDate, endDate);

        if ("MONTH".equalsIgnoreCase(groupBy)) {
            return calculateEventsPerMonth(events, startDate, endDate);
        }
        return calculateEventsPerWeek(events, startDate, endDate);
    }

    /**
     * Get time distribution by event type.
     */
    public Map<String, Double> getTimeDistribution(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<Event> events = getEventsInRange(userId, startDate, endDate);
        return calculateTimeDistribution(events);
    }

    /**
     * Get daily occupancy rate data.
     */
    public List<DailyOccupancy> getOccupancyRate(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<Event> events = getEventsInRange(userId, startDate, endDate);
        return calculateDailyOccupancy(events, startDate, endDate);
    }

    /**
     * Get busiest days heatmap data.
     */
    public List<DayHeatmapData> getBusiestDays(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<Event> events = getEventsInRange(userId, startDate, endDate);
        return buildWeeklyHeatmap(events);
    }

    /**
     * Get most used time slots distribution.
     */
    public List<TimeSlotData> getMostUsedTimeSlots(UUID userId) {
        List<Event> events = eventRepository.findAllByUserId(userId);
        return calculateTimeSlotDistribution(events);
    }

    /**
     * Get meeting vs individual work ratio.
     */
    public Map<String, Double> getMeetingVsWorkRatio(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<Event> events = getEventsInRange(userId, startDate, endDate);
        double meetingHours = calculateMeetingHours(events);
        double totalHours = calculateTotalHours(events);
        double personalHours = totalHours - meetingHours;

        Map<String, Double> result = new LinkedHashMap<>();
        result.put("meetingHours", round(meetingHours));
        result.put("personalHours", round(personalHours));
        result.put("totalHours", round(totalHours));
        result.put("meetingPercentage", totalHours > 0 ? round((meetingHours / totalHours) * 100) : 0);
        result.put("personalPercentage", totalHours > 0 ? round((personalHours / totalHours) * 100) : 0);

        return result;
    }

    /**
     * Get agenda conflicts in the given period.
     */
    public List<AgendaConflict> getAgendaConflicts(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<Event> events = getEventsInRange(userId, startDate, endDate);
        return detectConflicts(events);
    }

    /**
     * Get availability score (0-100).
     */
    public int getAvailabilityScore(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(30);
        List<Event> events = getEventsInRange(userId, today, endDate);

        double occupancyRate = calculateOccupancyRate(events, today, endDate);
        int conflictCount = detectConflicts(events).size();
        int eventCount = events.size();

        return calculateAvailabilityScore(occupancyRate, conflictCount, eventCount);
    }

    // --- Private helper methods ---

    private List<Event> getEventsInRange(UUID userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        return eventRepository.findAllByUserId(userId).stream()
                .filter(e -> !e.getEndTime().isBefore(startDateTime) && !e.getStartTime().isAfter(endDateTime))
                .toList();
    }

    private double calculateTotalHours(List<Event> events) {
        return events.stream()
                .mapToDouble(e -> ChronoUnit.MINUTES.between(e.getStartTime(), e.getEndTime()) / 60.0)
                .sum();
    }

    private List<EventsPerPeriod> calculateEventsPerWeek(List<Event> events, LocalDate startDate, LocalDate endDate) {
        WeekFields weekFields = WeekFields.ISO;
        Map<String, List<Event>> eventsByWeek = events.stream()
                .collect(Collectors.groupingBy(e -> {
                    LocalDate date = e.getStartTime().toLocalDate();
                    int week = date.get(weekFields.weekOfWeekBasedYear());
                    int year = date.get(weekFields.weekBasedYear());
                    return String.format("%d-W%02d", year, week);
                }));

        List<EventsPerPeriod> result = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            int week = current.get(weekFields.weekOfWeekBasedYear());
            int year = current.get(weekFields.weekBasedYear());
            String key = String.format("%d-W%02d", year, week);

            List<Event> weekEvents = eventsByWeek.getOrDefault(key, Collections.emptyList());
            double totalHours = calculateTotalHours(weekEvents);

            result.add(EventsPerPeriod.builder()
                    .period(key)
                    .label("Semaine " + week)
                    .eventCount(weekEvents.size())
                    .totalHours(round(totalHours))
                    .build());

            current = current.plusWeeks(1);
        }

        // Remove duplicates by period
        return result.stream()
                .collect(Collectors.toMap(
                        EventsPerPeriod::getPeriod,
                        e -> e,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ))
                .values().stream().toList();
    }

    private List<EventsPerPeriod> calculateEventsPerMonth(List<Event> events, LocalDate startDate, LocalDate endDate) {
        Map<String, List<Event>> eventsByMonth = events.stream()
                .collect(Collectors.groupingBy(e -> {
                    LocalDate date = e.getStartTime().toLocalDate();
                    return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                }));

        List<EventsPerPeriod> result = new ArrayList<>();
        YearMonth current = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);

        while (!current.isAfter(end)) {
            String key = current.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            List<Event> monthEvents = eventsByMonth.getOrDefault(key, Collections.emptyList());
            double totalHours = calculateTotalHours(monthEvents);

            String[] monthNames = {"", "Janvier", "Fevrier", "Mars", "Avril", "Mai", "Juin",
                    "Juillet", "Aout", "Septembre", "Octobre", "Novembre", "Decembre"};
            String label = monthNames[current.getMonthValue()] + " " + current.getYear();

            result.add(EventsPerPeriod.builder()
                    .period(key)
                    .label(label)
                    .eventCount(monthEvents.size())
                    .totalHours(round(totalHours))
                    .build());

            current = current.plusMonths(1);
        }

        return result;
    }

    private Map<String, Double> calculateTimeDistribution(List<Event> events) {
        double orgEventHours = events.stream()
                .filter(e -> e.getOrganizationId() != null)
                .mapToDouble(e -> ChronoUnit.MINUTES.between(e.getStartTime(), e.getEndTime()) / 60.0)
                .sum();

        double personalEventHours = events.stream()
                .filter(e -> e.getOrganizationId() == null)
                .mapToDouble(e -> ChronoUnit.MINUTES.between(e.getStartTime(), e.getEndTime()) / 60.0)
                .sum();

        Map<String, Double> distribution = new LinkedHashMap<>();
        distribution.put("Organization Events", round(orgEventHours));
        distribution.put("Personal Events", round(personalEventHours));

        return distribution;
    }

    private double calculateOccupancyRate(List<Event> events, LocalDate startDate, LocalDate endDate) {
        long totalWorkDays = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                totalWorkDays++;
            }
            current = current.plusDays(1);
        }

        double totalWorkHours = totalWorkDays * WORK_HOURS_PER_DAY;
        double scheduledHours = calculateTotalHours(events);

        return totalWorkHours > 0 ? (scheduledHours / totalWorkHours) * 100 : 0;
    }

    private List<DailyOccupancy> calculateDailyOccupancy(List<Event> events, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<Event>> eventsByDate = events.stream()
                .collect(Collectors.groupingBy(e -> e.getStartTime().toLocalDate()));

        List<DailyOccupancy> result = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            List<Event> dayEvents = eventsByDate.getOrDefault(current, Collections.emptyList());
            double occupied = calculateTotalHours(dayEvents);
            double available = WORK_HOURS_PER_DAY;
            double rate = (occupied / available) * 100;

            result.add(DailyOccupancy.builder()
                    .date(current.format(DATE_FORMATTER))
                    .occupiedHours(round(occupied))
                    .availableHours(available)
                    .occupancyRate(round(Math.min(rate, 100)))
                    .eventCount(dayEvents.size())
                    .build());

            current = current.plusDays(1);
        }

        return result;
    }

    private Map<String, Double> calculateBusiestDays(List<Event> events) {
        Map<DayOfWeek, List<Event>> eventsByDay = events.stream()
                .collect(Collectors.groupingBy(e -> e.getStartTime().getDayOfWeek()));

        Map<String, Double> result = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            List<Event> dayEvents = eventsByDay.getOrDefault(day, Collections.emptyList());
            double avgHours = dayEvents.isEmpty() ? 0 : calculateTotalHours(dayEvents) / Math.max(1, countOccurrences(events, day));
            result.put(day.name(), round(avgHours));
        }

        return result;
    }

    private int countOccurrences(List<Event> events, DayOfWeek targetDay) {
        Set<LocalDate> uniqueDates = events.stream()
                .map(e -> e.getStartTime().toLocalDate())
                .filter(d -> d.getDayOfWeek() == targetDay)
                .collect(Collectors.toSet());
        return uniqueDates.size();
    }

    private List<DayHeatmapData> buildWeeklyHeatmap(List<Event> events) {
        Map<DayOfWeek, List<Event>> eventsByDay = events.stream()
                .collect(Collectors.groupingBy(e -> e.getStartTime().getDayOfWeek()));

        List<DayHeatmapData> result = new ArrayList<>();
        String[] dayNames = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};

        for (int i = 0; i < 7; i++) {
            DayOfWeek day = DayOfWeek.of(i + 1);
            List<Event> dayEvents = eventsByDay.getOrDefault(day, Collections.emptyList());
            int occurrences = Math.max(1, countOccurrences(events, day));
            double avgHours = dayEvents.isEmpty() ? 0 : calculateTotalHours(dayEvents) / occurrences;
            double avgEvents = dayEvents.isEmpty() ? 0 : (double) dayEvents.size() / occurrences;

            String intensity;
            if (avgHours < 2) intensity = "LOW";
            else if (avgHours < 4) intensity = "MEDIUM";
            else if (avgHours < 6) intensity = "HIGH";
            else intensity = "VERY_HIGH";

            result.add(DayHeatmapData.builder()
                    .dayOfWeek(dayNames[i])
                    .dayIndex(i)
                    .averageHours(round(avgHours))
                    .averageEvents(round(avgEvents))
                    .intensity(intensity)
                    .build());
        }

        return result;
    }

    private List<TimeSlotData> calculateTimeSlotDistribution(List<Event> events) {
        Map<Integer, Long> countByHour = events.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getStartTime().getHour(),
                        Collectors.counting()
                ));

        long total = events.size();
        List<TimeSlotData> result = new ArrayList<>();

        for (int hour = 0; hour < 24; hour++) {
            long count = countByHour.getOrDefault(hour, 0L);
            double percentage = total > 0 ? (double) count / total * 100 : 0;

            result.add(TimeSlotData.builder()
                    .hour(hour)
                    .timeSlot(String.format("%02d:00-%02d:00", hour, (hour + 1) % 24))
                    .eventCount(count)
                    .percentage(round(percentage))
                    .build());
        }

        return result;
    }

    private String findMostUsedTimeSlot(List<TimeSlotData> distribution) {
        return distribution.stream()
                .max(Comparator.comparing(TimeSlotData::getEventCount))
                .map(TimeSlotData::getTimeSlot)
                .orElse("N/A");
    }

    private String findLeastUsedTimeSlot(List<TimeSlotData> distribution) {
        // Consider only work hours (9-18)
        return distribution.stream()
                .filter(d -> d.getHour() >= WORK_START_HOUR && d.getHour() < WORK_END_HOUR)
                .min(Comparator.comparing(TimeSlotData::getEventCount))
                .map(TimeSlotData::getTimeSlot)
                .orElse("N/A");
    }

    private double calculateMeetingHours(List<Event> events) {
        // Organization events are considered as meetings
        return events.stream()
                .filter(e -> e.getOrganizationId() != null)
                .mapToDouble(e -> ChronoUnit.MINUTES.between(e.getStartTime(), e.getEndTime()) / 60.0)
                .sum();
    }

    private List<AgendaConflict> detectConflicts(List<Event> events) {
        List<AgendaConflict> conflicts = new ArrayList<>();

        // Sort by start time
        List<Event> sortedEvents = events.stream()
                .sorted(Comparator.comparing(Event::getStartTime))
                .toList();

        for (int i = 0; i < sortedEvents.size(); i++) {
            Event event1 = sortedEvents.get(i);

            for (int j = i + 1; j < sortedEvents.size(); j++) {
                Event event2 = sortedEvents.get(j);

                // If event2 starts after event1 ends, no more conflicts possible for event1
                if (!event2.getStartTime().isBefore(event1.getEndTime())) {
                    break;
                }

                // Calculate overlap
                LocalDateTime overlapStart = event2.getStartTime();
                LocalDateTime overlapEnd = event1.getEndTime().isBefore(event2.getEndTime())
                        ? event1.getEndTime()
                        : event2.getEndTime();
                int overlapMinutes = (int) ChronoUnit.MINUTES.between(overlapStart, overlapEnd);

                if (overlapMinutes > 0) {
                    conflicts.add(AgendaConflict.builder()
                            .event1Id(event1.getId().toString())
                            .event1Title(event1.getTitle())
                            .event2Id(event2.getId().toString())
                            .event2Title(event2.getTitle())
                            .conflictDate(event1.getStartTime().toLocalDate().format(DATE_FORMATTER))
                            .conflictTime(overlapStart.format(TIME_FORMATTER) + "-" + overlapEnd.format(TIME_FORMATTER))
                            .overlapMinutes(overlapMinutes)
                            .build());
                }
            }
        }

        return conflicts;
    }

    private int calculateAvailabilityScore(double occupancyRate, int conflictCount, long eventCount) {
        // Base score starts at 100
        int score = 100;

        // Deduct based on occupancy rate (higher occupancy = lower availability)
        // Optimal occupancy is around 60-70%
        if (occupancyRate > 90) {
            score -= 40;
        } else if (occupancyRate > 80) {
            score -= 30;
        } else if (occupancyRate > 70) {
            score -= 15;
        } else if (occupancyRate > 60) {
            score -= 5;
        }

        // Deduct for conflicts
        score -= conflictCount * 10;

        // Very few events might indicate unused calendar
        if (eventCount < 5) {
            score = Math.max(score, 80); // Don't penalize too much for light schedules
        }

        return Math.max(0, Math.min(100, score));
    }

    private String generateAvailabilityInsight(int score, double occupancyRate) {
        if (score >= 80) {
            return "Excellente disponibilite. Votre agenda est bien equilibre.";
        } else if (score >= 60) {
            return "Bonne disponibilite. Quelques creneaux peuvent etre optimises.";
        } else if (score >= 40) {
            return "Disponibilite moderee. Considerez reorganiser certains evenements.";
        } else {
            return "Disponibilite faible. Votre agenda est tres charge avec des conflits potentiels.";
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
