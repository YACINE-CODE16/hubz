package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.OrganizationMember;
import com.hubz.infrastructure.persistence.entity.OrganizationMemberEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationMemberMapper {

    OrganizationMemberEntity toEntity(OrganizationMember member);

    OrganizationMember toDomain(OrganizationMemberEntity entity);
}
