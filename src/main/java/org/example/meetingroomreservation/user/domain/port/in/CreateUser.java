package org.example.meetingroomreservation.user.domain.port.in;

import org.example.meetingroomreservation.user.domain.model.User;

/**
 * Caso de uso de creacion de usuarios.
 */
public interface CreateUser {

    /**
     * @throws org.example.meetingroomreservation.user.domain.exception.EmailAlreadyExistsException
     *         si ya existe un usuario con ese correo, comparado tras normalizar
     */
    User create(CreateUserCommand command);
}
