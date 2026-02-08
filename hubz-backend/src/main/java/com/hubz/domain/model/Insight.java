package com.hubz.domain.model;

import com.hubz.domain.enums.InsightType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model representing a personal insight or recommendation for a user.
 * Insights are generated dynamically based on user activity patterns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Insight {

    /**
     * Unique identifier for the insight.
     */
    private UUID id;

    /**
     * The type of insight (e.g., productivity tip, habit suggestion, goal alert).
     */
    private InsightType type;

    /**
     * A short, descriptive title for the insight.
     */
    private String title;

    /**
     * The detailed message or recommendation.
     */
    private String message;

    /**
     * Priority level from 1 (low) to 5 (high).
     * Higher priority insights should be displayed more prominently.
     */
    private int priority;

    /**
     * Whether the insight has an associated action the user can take.
     */
    private boolean actionable;

    /**
     * Optional URL to navigate to when the user clicks on the insight.
     * Can be a relative path (e.g., "/personal/goals") or null if not actionable.
     */
    private String actionUrl;

    /**
     * When the insight was generated.
     */
    private LocalDateTime createdAt;
}
