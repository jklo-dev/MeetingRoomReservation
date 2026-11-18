package org.example.meetingroomreservation.reservation.domain.service;

import org.example.meetingroomreservation.reservation.domain.model.ReservationDetail;
import org.example.meetingroomreservation.reservation.domain.port.in.ListReservations;
import org.example.meetingroomreservation.reservation.domain.port.in.ListReservationsQuery;
import org.example.meetingroomreservation.reservation.domain.port.out.ReservationRepositoryPort;
import org.example.meetingroomreservation.shared.domain.PageResult;

/**
 * Implementacion del caso de uso de listado de reservas.
 *
 * <p>Deliberadamente delgada: la normalizacion y validacion de criterios ya las garantizan
 * {@code ReservationFilter} y {@code ListReservationsQuery} al construirse. Existe para que el
 * adaptador web no dependa del puerto de salida.
 */
public class ListReservationsService implements ListReservations {

    private final ReservationRepositoryPort reservations;

    public ListReservationsService(ReservationRepositoryPort reservations) {
        this.reservations = reservations;
    }

    @Override
    public PageResult<ReservationDetail> list(ListReservationsQuery query) {
        return reservations.search(query);
    }
}
