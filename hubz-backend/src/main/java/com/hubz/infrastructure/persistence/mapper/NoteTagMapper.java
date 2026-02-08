package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.NoteTag;
import com.hubz.infrastructure.persistence.entity.NoteTagEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NoteTagMapper {
    NoteTag toDomain(NoteTagEntity entity);
    NoteTagEntity toEntity(NoteTag domain);
}
