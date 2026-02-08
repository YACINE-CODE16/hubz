package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.GoalProgressHistory;
import com.hubz.infrastructure.persistence.entity.GoalProgressHistoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GoalProgressHistoryMapper {
    GoalProgressHistory toDomain(GoalProgressHistoryEntity entity);
    GoalProgressHistoryEntity toEntity(GoalProgressHistory domain);
}
