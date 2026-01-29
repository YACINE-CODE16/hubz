package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.Goal;
import com.hubz.infrastructure.persistence.entity.GoalEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GoalMapper {
    Goal toDomain(GoalEntity entity);
    GoalEntity toEntity(Goal domain);
}
