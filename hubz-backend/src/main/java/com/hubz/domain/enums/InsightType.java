package com.hubz.domain.enums;

/**
 * Types of personal insights that can be generated for users.
 */
public enum InsightType {
    /**
     * Tips to improve productivity based on patterns.
     * Example: "You complete most tasks on Tuesdays"
     */
    PRODUCTIVITY_TIP,

    /**
     * Suggestions related to habits.
     * Example: "Consider adding a morning routine habit"
     */
    HABIT_SUGGESTION,

    /**
     * Alerts about goal progress and deadlines.
     * Example: "Goal X is at risk - only 20% done with 3 days left"
     */
    GOAL_ALERT,

    /**
     * Warnings about workload.
     * Example: "You have 15 tasks due this week - consider delegation"
     */
    WORKLOAD_WARNING,

    /**
     * Celebrations for achievements.
     * Example: "Congratulations! You completed 50 tasks this month!"
     */
    CELEBRATION,

    /**
     * Detected patterns in user behavior.
     * Example: "You're most productive between 9-11am"
     */
    PATTERN_DETECTED
}
