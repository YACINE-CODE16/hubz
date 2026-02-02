package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.TaskComment;
import com.hubz.infrastructure.persistence.entity.TaskCommentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TaskCommentMapper {
    TaskComment toDomain(TaskCommentEntity entity);
    TaskCommentEntity toEntity(TaskComment domain);
}
