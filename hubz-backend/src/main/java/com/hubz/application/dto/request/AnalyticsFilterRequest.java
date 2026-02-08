package com.hubz.application.dto.request;

import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Filter parameters for analytics endpoints.
 * All fields are optional -- when null, no filtering is applied for that dimension.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsFilterRequest {

    /** Start of the date range (inclusive). Filters tasks by createdAt >= startDate. */
    private LocalDate startDate;

    /** End of the date range (inclusive). Filters tasks by createdAt <= endDate. */
    private LocalDate endDate;

    /** Filter by specific member/assignee IDs. */
    private List<UUID> memberIds;

    /** Filter by task status values. */
    private List<TaskStatus> statuses;

    /** Filter by task priority values. */
    private List<TaskPriority> priorities;

    /**
     * Check if any filter is active.
     */
    public boolean hasAnyFilter() {
        return startDate != null
                || endDate != null
                || (memberIds != null && !memberIds.isEmpty())
                || (statuses != null && !statuses.isEmpty())
                || (priorities != null && !priorities.isEmpty());
    }

    /**
     * Create an empty filter (no filtering applied).
     */
    public static AnalyticsFilterRequest empty() {
        return AnalyticsFilterRequest.builder().build();
    }
}
