package org.example.meetingroomreservation.reservation.domain.model;

/**
 * Estado de una reserva.
 *
 * <p>{@code CANCELLED} existe desde el principio aunque la creacion no pueda producirlo: la
 * columna necesita su restriccion y, sobre todo, el indice parcial de la restriccion de
 * exclusion filtra por {@code status = 'ACTIVE'}, asi que el valor debe existir en el esquema.
 */
public enum ReservationStatus {
    ACTIVE,
    CANCELLED
}
