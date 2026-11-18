package org.example.meetingroomreservation.reservation.domain.port.in;

import org.example.meetingroomreservation.reservation.domain.model.ReservationDetail;
import org.example.meetingroomreservation.shared.domain.PageResult;

/**
 * Caso de uso de consulta del listado de reservas.
 */
public interface ListReservations {

    PageResult<ReservationDetail> list(ListReservationsQuery query);
}
