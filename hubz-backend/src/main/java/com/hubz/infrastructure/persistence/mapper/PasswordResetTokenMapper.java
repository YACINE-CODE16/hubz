package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.PasswordResetToken;
import com.hubz.infrastructure.persistence.entity.PasswordResetTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PasswordResetTokenMapper {

    PasswordResetTokenEntity toEntity(PasswordResetToken domain);

    PasswordResetToken toDomain(PasswordResetTokenEntity entity);
}
