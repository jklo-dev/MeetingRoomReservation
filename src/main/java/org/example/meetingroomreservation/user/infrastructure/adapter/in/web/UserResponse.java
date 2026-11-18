package org.example.meetingroomreservation.user.infrastructure.adapter.in.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.example.meetingroomreservation.user.domain.model.Role;
import org.example.meetingroomreservation.user.domain.model.User;

/**
 * Representacion publica de un usuario.
 *
 * <p>No incluye la contrasena ni su hash: no aparecen en ninguna respuesta de la API.
 */
record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Role role,
        OffsetDateTime createdAt) {

    static UserResponse fromDomain(User user) {
        return new UserResponse(
                user.id(),
                user.email(),
                user.firstName(),
                user.lastName(),
                user.role(),
                user.createdAt());
    }
}
