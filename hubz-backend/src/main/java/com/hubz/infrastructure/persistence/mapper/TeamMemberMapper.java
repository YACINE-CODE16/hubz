package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.TeamMember;
import com.hubz.infrastructure.persistence.entity.TeamMemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TeamMemberMapper {
    TeamMember toDomain(TeamMemberEntity entity);
    TeamMemberEntity toEntity(TeamMember domain);
}
