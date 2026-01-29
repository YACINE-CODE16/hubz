package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.Habit;
import com.hubz.infrastructure.persistence.entity.HabitEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HabitMapper {
    Habit toDomain(HabitEntity entity);
    HabitEntity toEntity(Habit domain);
}
