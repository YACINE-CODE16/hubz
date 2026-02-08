package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaTaskRepository extends JpaRepository<TaskEntity, UUID> {

    List<TaskEntity> findByOrganizationId(UUID organizationId);

    List<TaskEntity> findByAssigneeId(UUID assigneeId);

    List<TaskEntity> findByGoalId(UUID goalId);

    @Query("SELECT t FROM TaskEntity t WHERE t.organizationId IN :orgIds AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<TaskEntity> searchByTitleOrDescription(@Param("query") String query, @Param("orgIds") List<UUID> organizationIds);

    // Productivity Stats Queries

    @Query("SELECT COUNT(t) FROM TaskEntity t WHERE t.assigneeId = :userId AND t.status = 'DONE' AND t.updatedAt >= :startDate AND t.updatedAt < :endDate")
    int countCompletedTasksByUserInRange(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(t) FROM TaskEntity t WHERE t.assigneeId = :userId AND t.createdAt >= :startDate AND t.createdAt < :endDate")
    int countTotalTasksByUserInRange(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, t.createdAt, t.updatedAt)) FROM TaskEntity t WHERE t.assigneeId = :userId AND t.status = 'DONE' AND t.updatedAt >= :startDate AND t.updatedAt < :endDate")
    Double getAverageCompletionTimeHours(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT CAST(t.updatedAt AS date), COUNT(t) FROM TaskEntity t WHERE t.assigneeId = :userId AND t.status = 'DONE' AND t.updatedAt >= :startDate AND t.updatedAt < :endDate GROUP BY CAST(t.updatedAt AS date) ORDER BY CAST(t.updatedAt AS date)")
    List<Object[]> getDailyCompletionCounts(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT DAYNAME(t.updatedAt), COUNT(t) as cnt FROM TaskEntity t WHERE t.assigneeId = :userId AND t.status = 'DONE' AND t.updatedAt >= :startDate AND t.updatedAt < :endDate GROUP BY DAYNAME(t.updatedAt) ORDER BY cnt DESC")
    List<Object[]> getMostProductiveDay(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t.priority, COUNT(t) FROM TaskEntity t WHERE t.assigneeId = :userId AND t.status = 'DONE' AND t.updatedAt >= :startDate AND t.updatedAt < :endDate GROUP BY t.priority")
    List<Object[]> countCompletedByPriority(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT DISTINCT CAST(t.updatedAt AS date) FROM TaskEntity t WHERE t.assigneeId = :userId AND t.status = 'DONE' AND t.updatedAt >= :startDate AND t.updatedAt < :endDate ORDER BY CAST(t.updatedAt AS date)")
    List<java.sql.Date> getProductiveDates(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Activity Heatmap Queries

    @Query("SELECT CAST(t.createdAt AS date), COUNT(t) FROM TaskEntity t WHERE t.creatorId = :userId AND t.createdAt >= :startDate AND t.createdAt < :endDate GROUP BY CAST(t.createdAt AS date)")
    List<Object[]> getDailyTaskCreationsByUser(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT CAST(t.updatedAt AS date), COUNT(t) FROM TaskEntity t WHERE t.organizationId = :orgId AND t.status = 'DONE' AND t.updatedAt >= :startDate AND t.updatedAt < :endDate GROUP BY CAST(t.updatedAt AS date)")
    List<Object[]> getDailyCompletionsByOrganization(@Param("orgId") UUID orgId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT CAST(t.updatedAt AS date), COUNT(t) FROM TaskEntity t WHERE t.organizationId = :orgId AND t.assigneeId = :userId AND t.status = 'DONE' AND t.updatedAt >= :startDate AND t.updatedAt < :endDate GROUP BY CAST(t.updatedAt AS date)")
    List<Object[]> getDailyCompletionsByOrganizationAndUser(@Param("orgId") UUID orgId, @Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Deadline Reminder Query
    @Query("SELECT t FROM TaskEntity t WHERE t.assigneeId = :assigneeId AND t.dueDate >= :start AND t.dueDate <= :end AND t.status != 'DONE' ORDER BY t.dueDate ASC")
    List<TaskEntity> findByAssigneeIdAndDueDateBetween(@Param("assigneeId") UUID assigneeId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
