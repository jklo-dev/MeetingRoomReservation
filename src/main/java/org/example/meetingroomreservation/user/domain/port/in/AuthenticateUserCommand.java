package org.example.meetingroomreservation.user.domain.port.in;

/**
 * Credenciales presentadas para autenticarse.
 */
public record AuthenticateUserCommand(String email, String rawPassword) {

    /**
     * Evita que la contrasena aparezca si el comando se imprime por error. La especificacion
     * exige que el cuerpo de la peticion de login no se registre en ningun nivel de log.
     */
    @Override
    public String toString() {
        return "AuthenticateUserCommand{email=" + email + "}";
    }
}
