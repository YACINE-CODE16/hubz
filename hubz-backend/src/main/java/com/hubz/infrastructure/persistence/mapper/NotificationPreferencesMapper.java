package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.NotificationPreferences;
import com.hubz.infrastructure.persistence.entity.NotificationPreferencesEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationPreferencesMapper {

    NotificationPreferencesEntity toEntity(NotificationPreferences domain);

    NotificationPreferences toDomain(NotificationPreferencesEntity entity);
}
