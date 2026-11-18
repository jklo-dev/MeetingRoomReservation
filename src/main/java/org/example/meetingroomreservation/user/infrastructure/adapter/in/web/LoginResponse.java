package org.example.meetingroomreservation.user.infrastructure.adapter.in.web;

import java.util.UUID;

import org.example.meetingroomreservation.user.domain.model.IssuedToken;
import org.example.meetingroomreservation.user.domain.model.Role;
import org.example.meetingroomreservation.user.domain.model.User;

/**
 * Respuesta del login: el token y quien es el usuario autenticado.
 *
 * <p>Incluye los datos del usuario porque no existe endpoint de usuario actual: sin ellos el
 * cliente tendria que decodificar el JWT por su cuenta para saber su propio rol.
 */
record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthenticatedUser user) {

    private static final String BEARER = "Bearer";

    /** Nunca incluye contrasena ni hash. */
    record AuthenticatedUser(
            UUID id,
            String email,
            String firstName,
            String lastName,
            Role role) {
    }

    static LoginResponse of(User user, IssuedToken token) {
        return new LoginResponse(
                token.value(),
                BEARER,
                token.expiresInSeconds(),
                new AuthenticatedUser(
                        user.id(),
                        user.email(),
                        user.firstName(),
                        user.lastName(),
                        user.role()));
    }

    /**
     * No incluye el token: esta respuesta podria acabar en un log.
     */
    @Override
    public String toString() {
        return "LoginResponse{tokenType=" + tokenType + ", expiresIn=" + expiresIn
                + ", user=" + (user != null ? user.email() : null) + "}";
    }
}
