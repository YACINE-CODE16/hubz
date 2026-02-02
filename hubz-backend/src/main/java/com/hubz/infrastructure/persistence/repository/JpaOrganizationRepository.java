package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaOrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {

    @Query("SELECT o FROM OrganizationEntity o WHERE LOWER(o.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<OrganizationEntity> searchByName(@Param("query") String query);
}
