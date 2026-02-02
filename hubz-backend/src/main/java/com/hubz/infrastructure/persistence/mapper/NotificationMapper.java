package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.Notification;
import com.hubz.infrastructure.persistence.entity.NotificationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationEntity toEntity(Notification notification);

    Notification toDomain(NotificationEntity entity);
}
