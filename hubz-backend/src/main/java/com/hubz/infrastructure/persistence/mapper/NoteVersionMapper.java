package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.NoteVersion;
import com.hubz.infrastructure.persistence.entity.NoteVersionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NoteVersionMapper {
    NoteVersion toDomain(NoteVersionEntity entity);
    NoteVersionEntity toEntity(NoteVersion domain);
}
