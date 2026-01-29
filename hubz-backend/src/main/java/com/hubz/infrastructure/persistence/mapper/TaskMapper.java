package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.Task;
import com.hubz.infrastructure.persistence.entity.TaskEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    TaskEntity toEntity(Task task);

    Task toDomain(TaskEntity entity);
}
