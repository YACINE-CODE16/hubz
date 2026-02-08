package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.TaskAttachment;
import com.hubz.infrastructure.persistence.entity.TaskAttachmentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskAttachmentMapper {
    TaskAttachment toDomain(TaskAttachmentEntity entity);
    TaskAttachmentEntity toEntity(TaskAttachment domain);
}
