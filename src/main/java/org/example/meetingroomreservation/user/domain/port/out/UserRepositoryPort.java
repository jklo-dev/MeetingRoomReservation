package org.example.meetingroomreservation.user.domain.port.out;

import org.example.meetingroomreservation.user.domain.model.User;

/**
 * Puerto de salida de persistencia de usuarios.
 *
 * <p>La funcionalidad 002 ampliara este puerto con la busqueda por correo.
 */
public interface UserRepositoryPort {

    /**
     * @param normalizedEmail correo ya normalizado por {@link User#normalizeEmail(String)}
     */
    boolean existsByEmail(String normalizedEmail);

    User save(User user);
}
