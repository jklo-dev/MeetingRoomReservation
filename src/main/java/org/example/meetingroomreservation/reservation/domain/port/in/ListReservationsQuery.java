package org.example.meetingroomreservation.reservation.domain.port.in;

import org.example.meetingroomreservation.reservation.domain.model.ReservationFilter;
import org.example.meetingroomreservation.shared.domain.PageRequest;
import org.example.meetingroomreservation.shared.domain.SortDirection;

/**
 * Consulta completa del listado de reservas.
 *
 * <p>No hay campo de ordenacion: el listado se ordena siempre por fecha de inicio, unico
 * criterio con sentido en una agenda, y solo se elige el sentido.
 */
public record ListReservationsQuery(
        ReservationFilter filter,
        SortDirection direction,
        PageRequest page) {

    public ListReservationsQuery {
        if (filter == null) {
            filter = ReservationFilter.none();
        }
        if (direction == null) {
            direction = SortDirection.ASC;
        }
        if (page == null) {
            page = PageRequest.firstPage();
        }
    }
}
