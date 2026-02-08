package com.hubz.domain.enums;

/**
 * Frequency at which deadline reminders are sent to users.
 */
public enum ReminderFrequency {
    /**
     * Send reminders 1 day before deadline only.
     */
    ONE_DAY,

    /**
     * Send reminders 3 days and 1 day before deadline.
     */
    THREE_DAYS,

    /**
     * Send reminders 7 days, 3 days, and 1 day before deadline.
     */
    ONE_WEEK
}
