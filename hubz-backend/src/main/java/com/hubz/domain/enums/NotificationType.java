package com.hubz.domain.enums;

public enum NotificationType {
    // Task notifications
    TASK_ASSIGNED,
    TASK_COMPLETED,
    TASK_DUE_SOON,
    TASK_OVERDUE,

    // Organization notifications
    ORGANIZATION_INVITE,
    ORGANIZATION_ROLE_CHANGED,
    ORGANIZATION_MEMBER_JOINED,
    ORGANIZATION_MEMBER_LEFT,

    // Goal notifications
    GOAL_DEADLINE_APPROACHING,
    GOAL_COMPLETED,
    GOAL_AT_RISK,

    // Event notifications
    EVENT_REMINDER,
    EVENT_UPDATED,
    EVENT_CANCELLED,

    // General
    MENTION,
    SYSTEM
}
