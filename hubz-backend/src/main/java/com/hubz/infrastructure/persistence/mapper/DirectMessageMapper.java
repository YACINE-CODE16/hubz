package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.DirectMessage;
import com.hubz.infrastructure.persistence.entity.DirectMessageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DirectMessageMapper {
    DirectMessage toDomain(DirectMessageEntity entity);
    DirectMessageEntity toEntity(DirectMessage domain);
}
