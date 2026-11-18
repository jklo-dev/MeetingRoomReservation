package org.example.meetingroomreservation.reservation.domain.port.in;

import java.util.UUID;

import org.example.meetingroomreservation.reservation.domain.model.Requester;

/**
 * Caso de uso de cancelacion de reservas.
 *
 * <p>Devuelve el mismo resultado compuesto que la creacion, porque la respuesta tiene la misma
 * forma: la reserva con su estado ya actualizado, mas la sala y el responsable anidados.
 */
public interface CancelReservation {

    CreateReservation.Result cancel(UUID reservationId, Requester requester);
}
