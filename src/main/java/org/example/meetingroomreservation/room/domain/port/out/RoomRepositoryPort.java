package org.example.meetingroomreservation.room.domain.port.out;

import org.example.meetingroomreservation.room.domain.model.Room;
import org.example.meetingroomreservation.room.domain.port.in.ListRoomsQuery;
import org.example.meetingroomreservation.shared.domain.PageResult;

/**
 * Puerto de salida de persistencia de salas.
 */
public interface RoomRepositoryPort {

    /**
     * @param normalizedName nombre ya normalizado por {@link Room#normalizedName(String)}
     */
    boolean existsByNormalizedName(String normalizedName);

    Room save(Room room);

    /**
     * Busca con filtros, orden y paginacion. El adaptador decide como traducir la consulta; el
     * dominio solo describe que quiere.
     */
    PageResult<Room> search(ListRoomsQuery query);
}
