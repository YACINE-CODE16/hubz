package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.User;
import com.hubz.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserEntity toEntity(User user);

    User toDomain(UserEntity entity);
}
