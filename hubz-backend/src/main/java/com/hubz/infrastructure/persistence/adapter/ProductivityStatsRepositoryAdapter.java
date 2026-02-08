package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.ProductivityStatsRepositoryPort;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.infrastructure.persistence.repository.JpaTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Adapter implementing the ProductivityStatsRepositoryPort.
 * Delegates to JpaTaskRepository for database queries.
 */
@Component
@RequiredArgsConstructor
public class ProductivityStatsRepositoryAdapter implements ProductivityStatsRepositoryPort {

    private final JpaTaskRepository taskRepository;

    @Override
    public int countCompletedTasksByUserInRange(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        return taskRepository.countCompletedTasksByUserInRange(userId, startDate, endDate);
    }

    @Override
    public int countTotalTasksByUserInRange(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        return taskRepository.countTotalTasksByUserInRange(userId, startDate, endDate);
    }

    @Override
    public Double getAverageCompletionTimeHours(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        return taskRepository.getAverageCompletionTimeHours(userId, startDate, endDate);
    }

    @Override
    public List<Object[]> getDailyCompletionCounts(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        return taskRepository.getDailyCompletionCounts(userId, startDate, endDate);
    }

    @Override
    public String getMostProductiveDay(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = taskRepository.getMostProductiveDay(userId, startDate, endDate);
        if (results.isEmpty()) {
            return null;
        }
        // Return the day with the highest count (first result due to ORDER BY cnt DESC)
        return (String) results.get(0)[0];
    }

    @Override
    public int[] countCompletedByPriority(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = taskRepository.countCompletedByPriority(userId, startDate, endDate);

        // Initialize counts for [URGENT, HIGH, MEDIUM, LOW]
        int[] counts = new int[4];

        for (Object[] row : results) {
            TaskPriority priority = (TaskPriority) row[0];
            Long count = (Long) row[1];

            if (priority != null) {
                switch (priority) {
                    case URGENT -> counts[0] = count.intValue();
                    case HIGH -> counts[1] = count.intValue();
                    case MEDIUM -> counts[2] = count.intValue();
                    case LOW -> counts[3] = count.intValue();
                }
            }
        }

        return counts;
    }

    @Override
    public List<LocalDateTime> getProductiveDates(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Date> dates = taskRepository.getProductiveDates(userId, startDate, endDate);
        return dates.stream()
                .map(date -> date.toLocalDate().atStartOfDay())
                .toList();
    }
}
