package org.example.meetingroomreservation.user.infrastructure.config;

import java.time.Clock;

import org.example.meetingroomreservation.user.domain.port.in.CreateUser;
import org.example.meetingroomreservation.user.domain.port.out.PasswordHasher;
import org.example.meetingroomreservation.user.domain.port.out.UserRepositoryPort;
import org.example.meetingroomreservation.user.domain.service.CreateUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado de los casos de uso de usuario.
 *
 * <p>Vive en infraestructura precisamente para que el dominio no tenga anotaciones de Spring.
 */
@Configuration
class UserUseCaseConfig {

    @Bean
    CreateUser createUser(UserRepositoryPort users, PasswordHasher passwordHasher, Clock clock) {
        return new CreateUserService(users, passwordHasher, clock);
    }
}
