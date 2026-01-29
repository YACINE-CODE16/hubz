package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.Team;
import com.hubz.infrastructure.persistence.entity.TeamEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TeamMapper {
    Team toDomain(TeamEntity entity);
    TeamEntity toEntity(Team domain);
}
