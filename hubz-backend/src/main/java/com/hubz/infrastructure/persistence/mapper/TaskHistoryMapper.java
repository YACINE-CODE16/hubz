package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.TaskHistory;
import com.hubz.infrastructure.persistence.entity.TaskHistoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskHistoryMapper {

    TaskHistoryEntity toEntity(TaskHistory taskHistory);

    TaskHistory toDomain(TaskHistoryEntity entity);
}
