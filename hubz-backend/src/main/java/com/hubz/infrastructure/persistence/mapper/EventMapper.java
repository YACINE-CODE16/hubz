package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.Event;
import com.hubz.infrastructure.persistence.entity.EventEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventMapper {
    Event toDomain(EventEntity entity);
    EventEntity toEntity(Event domain);
}
