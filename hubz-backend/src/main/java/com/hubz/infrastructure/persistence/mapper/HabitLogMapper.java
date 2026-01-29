package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.HabitLog;
import com.hubz.infrastructure.persistence.entity.HabitLogEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HabitLogMapper {
    HabitLog toDomain(HabitLogEntity entity);
    HabitLogEntity toEntity(HabitLog domain);
}
