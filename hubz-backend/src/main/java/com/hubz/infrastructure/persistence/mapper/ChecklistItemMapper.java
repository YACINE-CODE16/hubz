package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.ChecklistItem;
import com.hubz.infrastructure.persistence.entity.ChecklistItemEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChecklistItemMapper {
    ChecklistItem toDomain(ChecklistItemEntity entity);
    ChecklistItemEntity toEntity(ChecklistItem domain);
}
