package org.example.meetingroomreservation.room.domain.port.in;

import org.example.meetingroomreservation.room.domain.model.Room;
import org.example.meetingroomreservation.shared.domain.PageResult;

/**
 * Caso de uso de consulta del listado de salas.
 */
public interface ListRooms {

    PageResult<Room> list(ListRoomsQuery query);
}
