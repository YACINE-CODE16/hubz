package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.Note;
import com.hubz.infrastructure.persistence.entity.NoteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NoteMapper {
    Note toDomain(NoteEntity entity);
    NoteEntity toEntity(Note domain);
}
