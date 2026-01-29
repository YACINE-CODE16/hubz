package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.NoteAttachment;
import com.hubz.infrastructure.persistence.entity.NoteAttachmentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NoteAttachmentMapper {
    NoteAttachment toDomain(NoteAttachmentEntity entity);
    NoteAttachmentEntity toEntity(NoteAttachment domain);
}
