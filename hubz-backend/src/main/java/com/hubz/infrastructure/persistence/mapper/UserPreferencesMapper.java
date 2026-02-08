package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.UserPreferences;
import com.hubz.infrastructure.persistence.entity.UserPreferencesEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserPreferencesMapper {

    UserPreferencesEntity toEntity(UserPreferences domain);

    UserPreferences toDomain(UserPreferencesEntity entity);
}
