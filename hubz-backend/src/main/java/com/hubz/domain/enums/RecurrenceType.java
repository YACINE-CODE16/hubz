package com.hubz.domain.enums;

/**
 * Defines the recurrence type for events.
 */
public enum RecurrenceType {
    NONE,       // No recurrence - single event
    DAILY,      // Repeats every N days
    WEEKLY,     // Repeats every N weeks
    MONTHLY,    // Repeats every N months
    YEARLY      // Repeats every N years
}
