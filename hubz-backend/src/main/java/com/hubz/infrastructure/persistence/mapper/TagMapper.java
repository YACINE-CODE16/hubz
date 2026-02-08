package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.model.Tag;
import com.hubz.infrastructure.persistence.entity.TagEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {

    TagEntity toEntity(Tag tag);

    Tag toDomain(TagEntity entity);
}
