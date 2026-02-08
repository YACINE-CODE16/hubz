package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.EmailVerificationToken;
import com.hubz.infrastructure.persistence.entity.EmailVerificationTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmailVerificationTokenMapper {

    EmailVerificationTokenEntity toEntity(EmailVerificationToken domain);

    EmailVerificationToken toDomain(EmailVerificationTokenEntity entity);
}
