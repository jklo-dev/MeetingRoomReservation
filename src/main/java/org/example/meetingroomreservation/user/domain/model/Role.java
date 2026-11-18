package org.example.meetingroomreservation.user.domain.model;

/**
 * Rol de un usuario.
 *
 * <p>Se compara de forma sensible a mayusculas: {@code admin} en minusculas no es un valor
 * valido, segun la especificacion 001.
 */
public enum Role {
    USER,
    ADMIN
}
