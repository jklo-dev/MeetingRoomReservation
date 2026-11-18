package org.example.meetingroomreservation.room.domain.port.in;

import org.example.meetingroomreservation.room.domain.model.Room;

/**
 * Caso de uso de creacion de salas.
 */
public interface CreateRoom {

    /**
     * @throws org.example.meetingroomreservation.room.domain.exception.RoomNameAlreadyExistsException
     *         si ya existe una sala con ese nombre, comparado sin distinguir mayusculas
     */
    Room create(CreateRoomCommand command);
}
