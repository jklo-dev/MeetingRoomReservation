package org.example.meetingroomreservation.reservation.domain.port.in;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Datos necesarios para crear una reserva.
 *
 * <p>El responsable **no** viaja en el cuerpo de la peticion: lo pone el adaptador web a partir
 * del claim {@code sub} del token.
 */
public record CreateReservationCommand(
        UUID roomId,
        UUID ownerId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        int attendees) {
}
