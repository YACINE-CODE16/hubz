package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.GoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface GoalJpaRepository extends JpaRepository<GoalEntity, UUID> {
    List<GoalEntity> findByOrganizationId(UUID organizationId);
    List<GoalEntity> findByUserIdAndOrganizationIdIsNull(UUID userId);

    @Query("SELECT g FROM GoalEntity g WHERE (g.organizationId IN :orgIds OR (g.organizationId IS NULL AND g.userId = :userId)) AND LOWER(g.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<GoalEntity> searchByTitle(@Param("query") String query, @Param("orgIds") List<UUID> organizationIds, @Param("userId") UUID userId);

    List<GoalEntity> findByDeadline(LocalDate deadline);

    List<GoalEntity> findByDeadlineBetween(LocalDate start, LocalDate end);

    @Query("SELECT g FROM GoalEntity g WHERE g.userId = :userId AND g.organizationId IS NULL AND g.deadline >= :start AND g.deadline <= :end ORDER BY g.deadline ASC")
    List<GoalEntity> findPersonalGoalsByDeadlineBetween(@Param("userId") UUID userId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
