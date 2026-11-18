package org.example.meetingroomreservation.user.infrastructure.adapter.out.jpa;

import org.example.meetingroomreservation.user.domain.exception.EmailAlreadyExistsException;
import org.example.meetingroomreservation.user.domain.model.User;
import org.example.meetingroomreservation.user.domain.port.out.UserRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Adaptador de persistencia que satisface {@link UserRepositoryPort}.
 */
@Component
class UserJpaAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;

    UserJpaAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByEmail(String normalizedEmail) {
        return repository.existsByEmail(normalizedEmail);
    }

    @Override
    public User save(User user) {
        try {
            return repository.save(UserJpaEntity.fromDomain(user)).toDomain();
        } catch (DataIntegrityViolationException e) {
            // La comprobacion previa no cierra la ventana entre dos peticiones simultaneas con
            // el mismo correo: la restriccion unica es la que decide, y su violacion se traduce
            // a la misma excepcion de dominio para que el cliente reciba un 409.
            throw new EmailAlreadyExistsException();
        }
    }
}
