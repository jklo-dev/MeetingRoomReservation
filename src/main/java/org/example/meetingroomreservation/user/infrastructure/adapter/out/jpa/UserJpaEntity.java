package org.example.meetingroomreservation.user.infrastructure.adapter.out.jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.example.meetingroomreservation.user.domain.model.Role;
import org.example.meetingroomreservation.user.domain.model.User;

/**
 * Entidad de persistencia de usuario.
 *
 * <p>Es un artefacto de infraestructura: no se expone en la API ni se usa como modelo de
 * dominio. La traduccion hacia y desde {@link User} es explicita.
 */
@Entity
@Table(name = "users")
class UserJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected UserJpaEntity() {
        // Requerido por JPA.
    }

    static UserJpaEntity fromDomain(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.id = user.id();
        entity.email = user.email();
        entity.passwordHash = user.passwordHash();
        entity.firstName = user.firstName();
        entity.lastName = user.lastName();
        entity.role = user.role();
        entity.createdAt = user.createdAt();
        return entity;
    }

    User toDomain() {
        return User.restore(id, email, passwordHash, firstName, lastName, role, createdAt);
    }
}
