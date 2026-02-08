package com.hubz.domain.enums;

/**
 * Reminder times for events.
 */
public enum EventReminder {
    NONE(0),
    FIFTEEN_MINUTES(15),
    THIRTY_MINUTES(30),
    ONE_HOUR(60),
    TWO_HOURS(120),
    ONE_DAY(1440),
    TWO_DAYS(2880),
    ONE_WEEK(10080);

    private final int minutesBefore;

    EventReminder(int minutesBefore) {
        this.minutesBefore = minutesBefore;
    }

    public int getMinutesBefore() {
        return minutesBefore;
    }
}
