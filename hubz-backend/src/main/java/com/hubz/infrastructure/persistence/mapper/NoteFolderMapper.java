package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.NoteFolder;
import com.hubz.infrastructure.persistence.entity.NoteFolderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NoteFolderMapper {
    NoteFolder toDomain(NoteFolderEntity entity);
    NoteFolderEntity toEntity(NoteFolder domain);
}
