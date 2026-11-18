package org.example.meetingroomreservation.reservation.domain.model;

import java.util.UUID;

/**
 * Quien solicita una operacion sobre una reserva.
 *
 * <p>Lleva solo lo que la regla de autorizacion necesita: la identidad y si es administrador. No
 * importa el enumerado {@code Role} del modulo de usuarios, para no acoplar los modulos por algo
 * que aqui se reduce a un booleano.
 */
public record Requester(UUID id, boolean admin) {

    public boolean canCancel(Reservation reservation) {
        return admin || reservation.ownerId().equals(id);
    }
}
