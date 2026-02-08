package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.Message;
import com.hubz.infrastructure.persistence.entity.MessageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MessageMapper {
    Message toDomain(MessageEntity entity);
    MessageEntity toEntity(Message domain);
}
