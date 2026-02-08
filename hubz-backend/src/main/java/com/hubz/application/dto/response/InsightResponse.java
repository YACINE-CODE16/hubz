package com.hubz.application.dto.response;

import com.hubz.domain.enums.InsightType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for returning insight data to the API client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightResponse {

    /**
     * Unique identifier for the insight.
     */
    private UUID id;

    /**
     * The type of insight.
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
     */
    private int priority;

    /**
     * Whether the insight has an associated action.
     */
    private boolean actionable;

    /**
     * Optional URL for the action (can be a relative path).
     */
    private String actionUrl;

    /**
     * When the insight was generated.
     */
    private LocalDateTime createdAt;
}
