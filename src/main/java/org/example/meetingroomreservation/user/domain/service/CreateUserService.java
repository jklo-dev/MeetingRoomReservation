package org.example.meetingroomreservation.user.domain.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.example.meetingroomreservation.user.domain.exception.EmailAlreadyExistsException;
import org.example.meetingroomreservation.user.domain.model.User;
import org.example.meetingroomreservation.user.domain.port.in.CreateUser;
import org.example.meetingroomreservation.user.domain.port.in.CreateUserCommand;
import org.example.meetingroomreservation.user.domain.port.out.PasswordHasher;
import org.example.meetingroomreservation.user.domain.port.out.UserRepositoryPort;

/**
 * Implementacion del caso de uso de creacion de usuarios.
 *
 * <p>Sin anotaciones de Spring: el cableado se hace en la capa de infraestructura, de modo que
 * esta clase se puede probar con dobles y sin contexto.
 */
public class CreateUserService implements CreateUser {

    private final UserRepositoryPort users;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public CreateUserService(UserRepositoryPort users, PasswordHasher passwordHasher, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    public User create(CreateUserCommand command) {
        // Se normaliza antes de comprobar la unicidad, para que la comparacion use el mismo
        // valor que acabara almacenado.
        String normalizedEmail = User.normalizeEmail(command.email());

        if (users.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException();
        }

        String passwordHash = passwordHasher.hash(command.rawPassword());

        User user = User.create(
                normalizedEmail,
                passwordHash,
                command.firstName(),
                command.lastName(),
                command.role(),
                OffsetDateTime.now(clock));

        return users.save(user);
    }
}
