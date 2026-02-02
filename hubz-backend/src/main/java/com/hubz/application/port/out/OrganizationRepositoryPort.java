package com.hubz.application.port.out;

import com.hubz.domain.model.Organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepositoryPort {

    Organization save(Organization organization);

    Optional<Organization> findById(UUID id);

    List<Organization> findAll();

    void deleteById(UUID id);

    List<Organization> searchByName(String query);
}
