package org.example.meetingroomreservation.reservation.infrastructure.adapter.in.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.example.meetingroomreservation.reservation.domain.model.Reservation;
import org.example.meetingroomreservation.reservation.domain.model.ReservationOwner;
import org.example.meetingroomreservation.reservation.domain.model.ReservationStatus;
import org.example.meetingroomreservation.reservation.domain.model.RoomSnapshot;

/**
 * Representacion publica de una reserva.
 *
 * <p>Anida sala y responsable para que el cliente pueda mostrarla sin llamadas adicionales. La
 * sala incluye su capacidad, para poder presentar la ocupacion; el responsable **no** incluye
 * correo, porque el listado de reservas es publico.
 */
record ReservationResponse(
        UUID id,
        Room room,
        Owner owner,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        int attendees,
        ReservationStatus status,
        OffsetDateTime createdAt) {

    /** Nunca incluye el estado de la sala: no es informacion de la reserva. */
    record Room(UUID id, String name, int capacity) {
    }

    /** Nunca incluye el correo. */
    record Owner(UUID id, String firstName, String lastName) {
    }

    static ReservationResponse of(Reservation reservation, RoomSnapshot room,
                                  ReservationOwner owner) {
        return new ReservationResponse(
                reservation.id(),
                new Room(room.id(), room.name(), room.capacity()),
                new Owner(owner.id(), owner.firstName(), owner.lastName()),
                reservation.period().start(),
                reservation.period().end(),
                reservation.attendees(),
                reservation.status(),
                reservation.createdAt());
    }
}
