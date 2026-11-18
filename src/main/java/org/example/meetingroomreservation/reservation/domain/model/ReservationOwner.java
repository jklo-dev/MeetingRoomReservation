package org.example.meetingroomreservation.reservation.domain.model;

import java.util.UUID;

/**
 * Datos del responsable necesarios para representar una reserva.
 *
 * <p>**Sin correo**, deliberadamente: el listado de reservas es publico y publicar las
 * direcciones expondria el directorio de la organizacion.
 */
public record ReservationOwner(UUID id, String firstName, String lastName) {
}
