package com.hubz.application.port.out;

import com.hubz.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    User save(User user);

    boolean existsByEmail(String email);
}
