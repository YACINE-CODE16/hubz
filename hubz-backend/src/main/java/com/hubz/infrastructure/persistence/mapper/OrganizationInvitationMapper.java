package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.OrganizationInvitation;
import com.hubz.infrastructure.persistence.entity.OrganizationInvitationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationInvitationMapper {
    OrganizationInvitation toDomain(OrganizationInvitationEntity entity);
    OrganizationInvitationEntity toEntity(OrganizationInvitation domain);
}
