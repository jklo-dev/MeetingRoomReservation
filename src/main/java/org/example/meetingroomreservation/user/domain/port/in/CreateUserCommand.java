package org.example.meetingroomreservation.user.domain.port.in;

import org.example.meetingroomreservation.user.domain.model.Role;

/**
 * Datos necesarios para crear un usuario.
 *
 * <p>La contrasena viaja en claro hasta el caso de uso, que la delega al hasheador. No se
 * registra en ningun log.
 */
public record CreateUserCommand(
        String email,
        String rawPassword,
        String firstName,
        String lastName,
        Role role) {

    /**
     * Evita que la contrasena aparezca si el comando se imprime por error.
     */
    @Override
    public String toString() {
        return "CreateUserCommand{email=" + email + ", role=" + role + "}";
    }
}
