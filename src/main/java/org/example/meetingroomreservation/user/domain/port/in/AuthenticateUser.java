package org.example.meetingroomreservation.user.domain.port.in;

import org.example.meetingroomreservation.user.domain.model.IssuedToken;
import org.example.meetingroomreservation.user.domain.model.User;

/**
 * Caso de uso de autenticacion.
 */
public interface AuthenticateUser {

    /**
     * @throws org.example.meetingroomreservation.user.domain.exception.InvalidCredentialsException
     *         si el correo no existe o la contrasena es incorrecta, sin distinguir entre ambos
     */
    Result authenticate(AuthenticateUserCommand command);

    record Result(User user, IssuedToken token) {
    }
}
