package org.example.meetingroomreservation.reservation.domain.model;

import java.util.UUID;

/**
 * Lo que el modulo de reservas necesita saber de una sala, y nada mas.
 *
 * <p>No es la {@code Room} del modulo de salas: es una proyeccion propia con los tres datos que
 * las reglas de reserva requieren. Asi el modulo de reservas no depende del modelo de otro
 * modulo.
 */
public record RoomSnapshot(UUID id, String name, int capacity, boolean active) {
}
