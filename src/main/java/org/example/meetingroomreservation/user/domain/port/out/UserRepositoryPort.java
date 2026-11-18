package org.example.meetingroomreservation.user.domain.port.out;

import java.util.Optional;

import org.example.meetingroomreservation.user.domain.model.User;

/**
 * Puerto de salida de persistencia de usuarios.
 */
public interface UserRepositoryPort {

    /**
     * @param normalizedEmail correo ya normalizado por {@link User#normalizeEmail(String)}
     */
    boolean existsByEmail(String normalizedEmail);

    /**
     * @param normalizedEmail correo ya normalizado por {@link User#normalizeEmail(String)}
     */
    Optional<User> findByEmail(String normalizedEmail);

    User save(User user);
}
