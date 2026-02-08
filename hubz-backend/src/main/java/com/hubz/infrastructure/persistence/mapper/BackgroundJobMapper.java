package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.BackgroundJob;
import com.hubz.infrastructure.persistence.entity.BackgroundJobEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BackgroundJobMapper {
    BackgroundJob toDomain(BackgroundJobEntity entity);
    BackgroundJobEntity toEntity(BackgroundJob domain);
}
