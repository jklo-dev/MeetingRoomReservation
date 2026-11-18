package org.example.meetingroomreservation.room.domain.port.out;

import org.example.meetingroomreservation.room.domain.model.Room;

/**
 * Puerto de salida de persistencia de salas.
 */
public interface RoomRepositoryPort {

    /**
     * @param normalizedName nombre ya normalizado por {@link Room#normalizedName(String)}
     */
    boolean existsByNormalizedName(String normalizedName);

    Room save(Room room);
}
