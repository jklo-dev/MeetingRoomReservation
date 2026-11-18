package org.example.meetingroomreservation.reservation.domain.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.example.meetingroomreservation.reservation.domain.exception.ReservationExceptions;
import org.example.meetingroomreservation.reservation.domain.model.Reservation;
import org.example.meetingroomreservation.reservation.domain.model.ReservationOwner;
import org.example.meetingroomreservation.reservation.domain.model.Requester;
import org.example.meetingroomreservation.reservation.domain.model.RoomSnapshot;
import org.example.meetingroomreservation.reservation.domain.port.in.CancelReservation;
import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservation;
import org.example.meetingroomreservation.reservation.domain.port.out.OwnerLookupPort;
import org.example.meetingroomreservation.reservation.domain.port.out.ReservationRepositoryPort;
import org.example.meetingroomreservation.reservation.domain.port.out.RoomCalendarPort;

/**
 * Implementacion del caso de uso de cancelacion.
 *
 * <p><b>No necesita cerrojo</b>, al contrario que la creacion: afecta a una unica fila
 * identificada por clave primaria y no decide nada en funcion de la **ausencia** de otras filas.
 * No hay *write skew* posible. Dos cancelaciones simultaneas de la misma reserva producen un 200
 * y un 409, porque la segunda lee el estado ya cancelado.
 */
public class CancelReservationService implements CancelReservation {

    private final ReservationRepositoryPort reservations;
    private final RoomCalendarPort rooms;
    private final OwnerLookupPort owners;
    private final Clock clock;

    public CancelReservationService(ReservationRepositoryPort reservations,
                                    RoomCalendarPort rooms,
                                    OwnerLookupPort owners,
                                    Clock clock) {
        this.reservations = reservations;
        this.rooms = rooms;
        this.owners = owners;
        this.clock = clock;
    }

    @Override
    public CreateReservation.Result cancel(UUID reservationId, Requester requester) {
        // Paso 2: existencia.
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(ReservationExceptions.NotFound::new);

        // Paso 3: autorizacion ANTES que el estado y el tiempo. Al reves, un usuario sin permiso
        // podria distinguir "existe y esta activa" de "existe y esta cancelada" por el codigo de
        // error, y usar eso para sondear reservas ajenas.
        if (!requester.canCancel(reservation)) {
            throw new ReservationExceptions.NotAllowedToCancel();
        }

        // Pasos 4 a 6: la transicion valida el estado y el tiempo, y lanza la excepcion que
        // corresponda. La regla vive en el propio objeto.
        OffsetDateTime now = OffsetDateTime.now(clock);
        Reservation cancelled = reservation.cancel(requester.id(), now);

        Reservation saved = reservations.save(cancelled);

        return new CreateReservation.Result(saved, room(saved), owner(saved));
    }

    /**
     * Sin cerrojo: esta operacion no depende de la disponibilidad de la sala, solo necesita su
     * nombre y capacidad para componer la respuesta.
     */
    private RoomSnapshot room(Reservation reservation) {
        return rooms.findWithoutLock(reservation.roomId())
                .orElseThrow(() -> new IllegalStateException(
                        "la sala de una reserva existente no existe: " + reservation.roomId()));
    }

    private ReservationOwner owner(Reservation reservation) {
        return owners.findById(reservation.ownerId())
                .orElseThrow(() -> new IllegalStateException(
                        "el responsable de una reserva existente no existe: "
                                + reservation.ownerId()));
    }
}
