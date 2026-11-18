package org.example.meetingroomreservation.user.domain.service;

import java.util.Optional;

import org.example.meetingroomreservation.user.domain.exception.InvalidCredentialsException;
import org.example.meetingroomreservation.user.domain.model.User;
import org.example.meetingroomreservation.user.domain.port.in.AuthenticateUser;
import org.example.meetingroomreservation.user.domain.port.in.AuthenticateUserCommand;
import org.example.meetingroomreservation.user.domain.port.out.PasswordHasher;
import org.example.meetingroomreservation.user.domain.port.out.TokenIssuer;
import org.example.meetingroomreservation.user.domain.port.out.UserRepositoryPort;

/**
 * Implementacion del caso de uso de autenticacion.
 *
 * <p>Sin anotaciones de Spring: el cableado vive en infraestructura.
 */
public class AuthenticateUserService implements AuthenticateUser {

    private final UserRepositoryPort users;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public AuthenticateUserService(UserRepositoryPort users, PasswordHasher passwordHasher,
                                   TokenIssuer tokenIssuer) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public Result authenticate(AuthenticateUserCommand command) {
        String normalizedEmail = normalize(command.email());
        Optional<User> found = users.findByEmail(normalizedEmail);

        if (found.isEmpty()) {
            // Se gasta el mismo tiempo que en una verificacion real antes de rechazar. Sin esto,
            // un correo inexistente responderia perceptiblemente mas rapido y la diferencia de
            // latencia permitiria enumerar cuentas.
            passwordHasher.matchesDecoy(command.rawPassword());
            throw new InvalidCredentialsException();
        }

        User user = found.get();
        if (!passwordHasher.matches(command.rawPassword(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        return new Result(user, tokenIssuer.issue(user));
    }

    /**
     * Normaliza como lo hace la creacion, pero sin propagar el fallo de validacion: un correo
     * vacio o demasiado largo no es una peticion invalida aqui, es una credencial que no existe,
     * y debe responder 401 igual que cualquier otro correo desconocido.
     */
    private String normalize(String email) {
        try {
            return User.normalizeEmail(email);
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialsException();
        }
    }
}
