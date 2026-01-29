package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.Organization;
import com.hubz.infrastructure.persistence.entity.OrganizationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    OrganizationEntity toEntity(Organization organization);

    Organization toDomain(OrganizationEntity entity);
}
