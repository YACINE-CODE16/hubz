package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.GoalProgressHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoalProgressHistoryJpaRepository extends JpaRepository<GoalProgressHistoryEntity, UUID> {

    List<GoalProgressHistoryEntity> findByGoalIdOrderByRecordedAtAsc(UUID goalId);

    List<GoalProgressHistoryEntity> findByGoalIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            UUID goalId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT h FROM GoalProgressHistoryEntity h WHERE h.goalId = :goalId ORDER BY h.recordedAt DESC LIMIT 1")
    Optional<GoalProgressHistoryEntity> findLatestByGoalId(@Param("goalId") UUID goalId);

    void deleteByGoalId(UUID goalId);

    // Activity Heatmap methods
    @Query("SELECT CAST(h.recordedAt AS date), COUNT(h) FROM GoalProgressHistoryEntity h WHERE h.goalId IN :goalIds AND h.recordedAt >= :startDate AND h.recordedAt < :endDate GROUP BY CAST(h.recordedAt AS date)")
    List<Object[]> getDailyUpdatesByGoalIds(@Param("goalIds") List<UUID> goalIds, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
