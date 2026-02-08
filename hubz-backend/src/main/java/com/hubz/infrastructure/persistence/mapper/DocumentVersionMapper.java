package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.DocumentVersion;
import com.hubz.infrastructure.persistence.entity.DocumentVersionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentVersionMapper {
    DocumentVersion toDomain(DocumentVersionEntity entity);
    DocumentVersionEntity toEntity(DocumentVersion domain);
}
