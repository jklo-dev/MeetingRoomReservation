package org.example.meetingroomreservation.reservation.domain.model;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Criterios de busqueda del listado de reservas. Todos opcionales y acumulativos.
 *
 * <p><b>Semantica del rango</b>: {@code from} y {@code to} delimitan un intervalo y se devuelven
 * las reservas que **se solapan** con el, no las contenidas en el. Basta con que la reserva toque
 * el rango en algun punto:
 *
 * <ul>
 *   <li>Solo {@code from} → el <b>fin</b> de la reserva es posterior a {@code from}.</li>
 *   <li>Solo {@code to} → el <b>inicio</b> de la reserva es anterior a {@code to}.</li>
 *   <li>Ambos → las dos condiciones a la vez.</li>
 * </ul>
 *
 * <p>Una reserva de 10:00 a 12:00 **si** aparece consultando desde las 11:00, porque a esa hora
 * la sala esta ocupada. Comparar contra el inicio la habria ocultado y el listado dejaria de
 * servir para saber que salas estan en uso.
 */
public record ReservationFilter(
        UUID roomId,
        UUID ownerId,
        ReservationStatusFilter status,
        OffsetDateTime from,
        OffsetDateTime to) {

    public ReservationFilter {
        if (status == null) {
            status = ReservationStatusFilter.DEFAULT;
        }
        if (from != null && to != null && !to.isAfter(from)) {
            throw new IllegalArgumentException("to debe ser posterior a from");
        }
    }

    public static ReservationFilter none() {
        return new ReservationFilter(null, null, ReservationStatusFilter.DEFAULT, null, null);
    }

    public Optional<UUID> roomIdValue() {
        return Optional.ofNullable(roomId);
    }

    public Optional<UUID> ownerIdValue() {
        return Optional.ofNullable(ownerId);
    }

    public Optional<OffsetDateTime> fromValue() {
        return Optional.ofNullable(from);
    }

    public Optional<OffsetDateTime> toValue() {
        return Optional.ofNullable(to);
    }
}
