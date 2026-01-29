package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.OrganizationDocument;
import com.hubz.infrastructure.persistence.entity.OrganizationDocumentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationDocumentMapper {
    OrganizationDocument toDomain(OrganizationDocumentEntity entity);
    OrganizationDocumentEntity toEntity(OrganizationDocument domain);
}
