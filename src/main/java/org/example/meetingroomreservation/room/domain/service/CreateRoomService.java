package org.example.meetingroomreservation.room.domain.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.example.meetingroomreservation.room.domain.exception.RoomNameAlreadyExistsException;
import org.example.meetingroomreservation.room.domain.model.Room;
import org.example.meetingroomreservation.room.domain.port.in.CreateRoom;
import org.example.meetingroomreservation.room.domain.port.in.CreateRoomCommand;
import org.example.meetingroomreservation.room.domain.port.out.RoomRepositoryPort;

/**
 * Implementacion del caso de uso de creacion de salas.
 *
 * <p>Sin anotaciones de Spring: el cableado vive en infraestructura.
 */
public class CreateRoomService implements CreateRoom {

    private final RoomRepositoryPort rooms;
    private final Clock clock;

    public CreateRoomService(RoomRepositoryPort rooms, Clock clock) {
        this.rooms = rooms;
        this.clock = clock;
    }

    @Override
    public Room create(CreateRoomCommand command) {
        // La comprobacion usa la forma normalizada, la misma que indexa la base de datos.
        if (rooms.existsByNormalizedName(Room.normalizedName(command.name()))) {
            throw new RoomNameAlreadyExistsException();
        }

        Room room = Room.create(command.name(), command.capacity(), OffsetDateTime.now(clock));

        return rooms.save(room);
    }
}
