package org.example.meetingroomreservation.room.domain.exception;

/**
 * Ya existe una sala con el nombre indicado, comparado sin distinguir mayusculas.
 */
public class RoomNameAlreadyExistsException extends RuntimeException {

    public RoomNameAlreadyExistsException() {
        super("Ya existe una sala con el nombre indicado");
    }
}
