package org.example.meetingroomreservation.user.domain.exception;

/**
 * Credenciales invalidas.
 *
 * <p>Deliberadamente **no** distingue la causa: correo inexistente y contrasena incorrecta
 * lanzan esta misma excepcion con el mismo mensaje, para que la respuesta sea identica y no se
 * pueda averiguar que correos estan registrados.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciales invalidas");
    }
}
