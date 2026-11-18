package org.example.meetingroomreservation.reservation.infrastructure.config;

import java.time.Clock;
import java.util.UUID;

import org.example.meetingroomreservation.reservation.domain.model.Requester;
import org.example.meetingroomreservation.reservation.domain.port.in.CancelReservation;
import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservation;
import org.example.meetingroomreservation.reservation.domain.port.out.OwnerLookupPort;
import org.example.meetingroomreservation.reservation.domain.port.out.ReservationRepositoryPort;
import org.example.meetingroomreservation.reservation.domain.port.out.RoomCalendarPort;
import org.example.meetingroomreservation.reservation.domain.service.CancelReservationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Envuelve el caso de uso de cancelacion en una transaccion.
 *
 * <p>A diferencia de la creacion, aqui **no** se fija el aislamiento: no hay cerrojo cuyo
 * comportamiento dependa de el. La transaccion existe para que la lectura y la actualizacion de
 * la reserva sean atomicas.
 */
@Component
class TransactionalCancelReservation implements CancelReservation {

    private final CancelReservationService delegate;

    TransactionalCancelReservation(ReservationRepositoryPort reservations,
                                   RoomCalendarPort rooms,
                                   OwnerLookupPort owners,
                                   Clock clock) {
        this.delegate = new CancelReservationService(reservations, rooms, owners, clock);
    }

    @Override
    @Transactional
    public CreateReservation.Result cancel(UUID reservationId, Requester requester) {
        return delegate.cancel(reservationId, requester);
    }
}
