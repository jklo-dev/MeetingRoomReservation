package org.example.meetingroomreservation.reservation.domain.port.in;

import org.example.meetingroomreservation.reservation.domain.model.Reservation;
import org.example.meetingroomreservation.reservation.domain.model.ReservationOwner;
import org.example.meetingroomreservation.reservation.domain.model.RoomSnapshot;

/**
 * Caso de uso de creacion de reservas.
 */
public interface CreateReservation {

    Result create(CreateReservationCommand command);

    /**
     * Devuelve tambien la sala y el responsable porque la respuesta los anida, y el caso de uso
     * ya los ha consultado: obligar al adaptador web a volver a buscarlos seria redundante y le
     * daria acceso a puertos que no le corresponden.
     */
    record Result(Reservation reservation, RoomSnapshot room, ReservationOwner owner) {
    }
}
