package org.example.meetingroomreservation.room.domain.port.in;

import org.example.meetingroomreservation.room.domain.model.RoomFilter;
import org.example.meetingroomreservation.room.domain.model.RoomSortField;
import org.example.meetingroomreservation.shared.domain.PageRequest;
import org.example.meetingroomreservation.shared.domain.SortDirection;

/**
 * Consulta completa del listado de salas: filtros, orden y paginacion.
 */
public record ListRoomsQuery(
        RoomFilter filter,
        RoomSortField sortBy,
        SortDirection direction,
        PageRequest page) {

    public ListRoomsQuery {
        if (filter == null) {
            filter = RoomFilter.none();
        }
        if (sortBy == null) {
            sortBy = RoomSortField.DEFAULT;
        }
        if (direction == null) {
            direction = SortDirection.ASC;
        }
        if (page == null) {
            page = PageRequest.firstPage();
        }
    }
}
