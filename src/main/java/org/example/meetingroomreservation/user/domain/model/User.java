package org.example.meetingroomreservation.user.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Usuario de la aplicacion.
 *
 * <p>Garantiza sus invariantes al construirse: el correo llega ya normalizado, el nombre y el
 * apellido no estan vacios y el hash de la contrasena esta presente. La contrasena en claro
 * nunca entra en este objeto.
 */
public final class User {

    public static final int MAX_EMAIL_LENGTH = 255;
    public static final int MAX_NAME_LENGTH = 100;

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String firstName;
    private final String lastName;
    private final Role role;
    private final OffsetDateTime createdAt;

    private User(UUID id, String email, String passwordHash, String firstName, String lastName,
                 Role role, OffsetDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.createdAt = createdAt;
    }

    /**
     * Crea un usuario nuevo, generando su identificador en el dominio.
     *
     * <p>El correo se normaliza aqui y no en el controlador, para que cualquier via de entrada
     * futura aplique la misma regla.
     */
    public static User create(String email, String passwordHash, String firstName, String lastName,
                             Role role, OffsetDateTime createdAt) {
        return restore(UUID.randomUUID(), email, passwordHash, firstName, lastName, role, createdAt);
    }

    /**
     * Reconstruye un usuario existente, por ejemplo al leerlo de la persistencia.
     */
    public static User restore(UUID id, String email, String passwordHash, String firstName,
                               String lastName, Role role, OffsetDateTime createdAt) {
        Objects.requireNonNull(id, "el identificador es obligatorio");
        Objects.requireNonNull(role, "el rol es obligatorio");
        Objects.requireNonNull(createdAt, "el instante de creacion es obligatorio");

        String normalizedEmail = normalizeEmail(email);
        String trimmedFirstName = requireText(firstName, "el nombre", MAX_NAME_LENGTH);
        String trimmedLastName = requireText(lastName, "el apellido", MAX_NAME_LENGTH);

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("el hash de la contrasena es obligatorio");
        }

        return new User(id, normalizedEmail, passwordHash, trimmedFirstName, trimmedLastName, role,
                createdAt);
    }

    /**
     * Normaliza un correo: recorta espacios externos y lo pasa a minusculas.
     *
     * <p>Es publico porque la comprobacion de unicidad debe usar exactamente la misma regla que
     * el almacenamiento.
     */
    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("el correo es obligatorio");
        }
        String normalized = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException(
                    "el correo no puede exceder " + MAX_EMAIL_LENGTH + " caracteres");
        }
        return normalized;
    }

    private static String requireText(String value, String campo, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(campo + " no puede exceder " + maxLength + " caracteres");
        }
        return trimmed;
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public Role role() {
        return role;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    /**
     * No incluye el hash de la contrasena: este objeto puede acabar en un log.
     */
    @Override
    public String toString() {
        return "User{id=" + id + ", email=" + email + ", role=" + role + "}";
    }
}
