package org.example.meetingroomreservation.reservation.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import org.example.meetingroomreservation.reservation.domain.model.ReservationOwner;

/**
 * Consulta de los datos del responsable necesarios para representar una reserva.
 *
 * <p>Igual que {@code RoomCalendarPort}, es un puerto propio del modulo de reservas con
 * exactamente lo que necesita, en lugar de depender del modelo del modulo de usuarios.
 */
public interface OwnerLookupPort {

    Optional<ReservationOwner> findById(UUID ownerId);
}
