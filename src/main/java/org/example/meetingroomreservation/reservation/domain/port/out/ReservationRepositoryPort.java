package org.example.meetingroomreservation.reservation.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import org.example.meetingroomreservation.reservation.domain.model.Reservation;
import org.example.meetingroomreservation.reservation.domain.model.ReservationDetail;
import org.example.meetingroomreservation.reservation.domain.model.TimeRange;
import org.example.meetingroomreservation.reservation.domain.port.in.ListReservationsQuery;
import org.example.meetingroomreservation.shared.domain.PageResult;

/**
 * Puerto de salida de persistencia de reservas.
 */
public interface ReservationRepositoryPort {

    /**
     * Indica si existe alguna reserva **activa** de esa sala que se solape con el intervalo.
     *
     * <p>Por si sola no garantiza la invariante: entre esta consulta y la insercion cabe otra
     * transaccion. Existe para producir un 409 con mensaje util en el camino normal; la garantia
     * la aportan el cerrojo y la restriccion de exclusion.
     */
    boolean existsActiveOverlapping(UUID roomId, TimeRange period);

    Optional<Reservation> findById(UUID reservationId);

    /**
     * Busca con filtros, orden y paginacion, trayendo la sala y el responsable de cada reserva.
     *
     * <p>Devuelve {@code ReservationDetail} y no {@code Reservation} a proposito: el adaptador
     * puede resolverlo con una sola consulta, mientras que devolver solo las reservas obligaria
     * a consultar sala y responsable uno a uno.
     */
    PageResult<ReservationDetail> search(ListReservationsQuery query);

    /**
     * @throws org.example.meetingroomreservation.reservation.domain.exception.ReservationExceptions.Overlap
     *         si la restriccion de exclusion rechaza la insercion
     */
    Reservation save(Reservation reservation);
}
