package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.EventParticipant;
import com.hubz.infrastructure.persistence.entity.EventParticipantEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventParticipantMapper {
    EventParticipant toDomain(EventParticipantEntity entity);
    EventParticipantEntity toEntity(EventParticipant domain);
}
