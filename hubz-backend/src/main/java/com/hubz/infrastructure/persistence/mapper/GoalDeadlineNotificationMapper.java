package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.GoalDeadlineNotification;
import com.hubz.infrastructure.persistence.entity.GoalDeadlineNotificationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GoalDeadlineNotificationMapper {

    GoalDeadlineNotification toDomain(GoalDeadlineNotificationEntity entity);

    GoalDeadlineNotificationEntity toEntity(GoalDeadlineNotification domain);
}
