package org.example.meetingroomreservation.user.domain.exception;

/**
 * Ya existe un usuario con el correo indicado.
 *
 * <p>No expone el correo en el mensaje que llega al cliente: el cuerpo del 409 no revela datos
 * del usuario existente.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super("Ya existe un usuario con el correo indicado");
    }
}
