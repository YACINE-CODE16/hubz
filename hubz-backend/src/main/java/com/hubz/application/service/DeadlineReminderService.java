package com.hubz.application.service;

import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.ReminderFrequency;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.Event;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.Task;
import com.hubz.domain.model.User;
import com.hubz.domain.model.UserPreferences;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Service for generating and sending deadline reminder emails to users.
 * Checks for upcoming tasks, goals, and events and sends reminder emails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeadlineReminderService {

    private final UserRepositoryPort userRepository;
    private final TaskRepositoryPort taskRepository;
    private final GoalRepositoryPort goalRepository;
    private final EventRepositoryPort eventRepository;
    private final EmailService emailService;

    /**
     * Represents an upcoming deadline item.
     */
    public record DeadlineItem(
            String type,
            String title,
            LocalDate dueDate,
            String urgency,
            UUID itemId,
            UUID organizationId
    ) {}

    /**
     * Data class for deadline reminder content.
     */
    public record DeadlineReminderData(
            List<DeadlineItem> todayItems,
            List<DeadlineItem> thisWeekItems,
            List<DeadlineItem> nextWeekItems
    ) {
        public boolean hasReminders() {
            return !todayItems.isEmpty() || !thisWeekItems.isEmpty() || !nextWeekItems.isEmpty();
        }

        public int totalCount() {
            return todayItems.size() + thisWeekItems.size() + nextWeekItems.size();
        }
    }

    /**
     * Generate deadline reminders for a user based on their reminder frequency preference.
     *
     * @param userId the user ID
     * @param preferences the user's preferences containing reminder frequency
     * @return DeadlineReminderData containing all upcoming deadlines grouped by urgency
     */
    public DeadlineReminderData generateReminders(UUID userId, UserPreferences preferences) {
        LocalDate today = LocalDate.now();
        ReminderFrequency frequency = preferences.getReminderFrequency();

        // Determine date range based on frequency
        LocalDate endDate = switch (frequency) {
            case ONE_DAY -> today.plusDays(1);
            case THREE_DAYS -> today.plusDays(3);
            case ONE_WEEK -> today.plusDays(7);
        };

        List<DeadlineItem> allItems = new ArrayList<>();

        // Get tasks with due dates in range
        List<Task> tasks = taskRepository.findByAssigneeIdAndDueDateBetween(
                userId,
                today.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        );
        for (Task task : tasks) {
            if (task.getStatus() != TaskStatus.DONE && task.getDueDate() != null) {
                LocalDate dueDate = task.getDueDate().toLocalDate();
                allItems.add(new DeadlineItem(
                        "Tache",
                        task.getTitle(),
                        dueDate,
                        calculateUrgency(dueDate, today),
                        task.getId(),
                        task.getOrganizationId()
                ));
            }
        }

        // Get personal goals with deadlines in range
        List<Goal> goals = goalRepository.findPersonalGoalsByDeadlineBetween(userId, today, endDate);
        for (Goal goal : goals) {
            if (goal.getDeadline() != null) {
                allItems.add(new DeadlineItem(
                        "Objectif",
                        goal.getTitle(),
                        goal.getDeadline(),
                        calculateUrgency(goal.getDeadline(), today),
                        goal.getId(),
                        goal.getOrganizationId()
                ));
            }
        }

        // Get events starting in range
        List<Event> events = eventRepository.findPersonalEventsByTimeRange(
                userId,
                today.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        );
        for (Event event : events) {
            if (event.getStartTime() != null) {
                LocalDate eventDate = event.getStartTime().toLocalDate();
                allItems.add(new DeadlineItem(
                        "Evenement",
                        event.getTitle(),
                        eventDate,
                        calculateUrgency(eventDate, today),
                        event.getId(),
                        event.getOrganizationId()
                ));
            }
        }

        // Sort by due date
        allItems.sort(Comparator.comparing(DeadlineItem::dueDate));

        // Group by urgency
        List<DeadlineItem> todayItems = new ArrayList<>();
        List<DeadlineItem> thisWeekItems = new ArrayList<>();
        List<DeadlineItem> nextWeekItems = new ArrayList<>();

        LocalDate oneWeekFromNow = today.plusDays(7);

        for (DeadlineItem item : allItems) {
            if (item.dueDate().equals(today) || item.dueDate().equals(today.plusDays(1))) {
                todayItems.add(item);
            } else if (item.dueDate().isBefore(oneWeekFromNow)) {
                thisWeekItems.add(item);
            } else {
                nextWeekItems.add(item);
            }
        }

        return new DeadlineReminderData(todayItems, thisWeekItems, nextWeekItems);
    }

    /**
     * Calculate urgency level based on due date.
     */
    private String calculateUrgency(LocalDate dueDate, LocalDate today) {
        long daysUntil = today.until(dueDate).getDays();
        if (daysUntil <= 0) {
            return "Aujourd'hui";
        } else if (daysUntil == 1) {
            return "Demain";
        } else if (daysUntil <= 3) {
            return "Dans " + daysUntil + " jours";
        } else if (daysUntil <= 7) {
            return "Cette semaine";
        } else {
            return "La semaine prochaine";
        }
    }

    /**
     * Send deadline reminder email to a user.
     *
     * @param userId the user ID
     * @param preferences the user's preferences
     */
    public void sendDeadlineReminder(UUID userId, UserPreferences preferences) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Cannot send deadline reminder: user {} not found", userId);
            return;
        }

        try {
            DeadlineReminderData reminders = generateReminders(userId, preferences);

            if (!reminders.hasReminders()) {
                log.debug("No deadline reminders for user {} - skipping email", userId);
                return;
            }

            emailService.sendDeadlineReminderEmail(
                    user.getEmail(),
                    user.getFirstName(),
                    reminders
            );

            log.info("Deadline reminder sent to user {} ({}) with {} items",
                    userId, user.getEmail(), reminders.totalCount());
        } catch (Exception e) {
            log.error("Failed to send deadline reminder to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Get reminder days based on frequency.
     * Used by scheduler to determine which days to check.
     *
     * @param frequency the reminder frequency
     * @return array of days before deadline to send reminders
     */
    public static int[] getReminderDaysForFrequency(ReminderFrequency frequency) {
        return switch (frequency) {
            case ONE_DAY -> new int[]{1};
            case THREE_DAYS -> new int[]{3, 1};
            case ONE_WEEK -> new int[]{7, 3, 1};
        };
    }
}
