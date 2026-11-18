package org.example.meetingroomreservation.room.domain.service;

import org.example.meetingroomreservation.room.domain.model.Room;
import org.example.meetingroomreservation.room.domain.port.in.ListRooms;
import org.example.meetingroomreservation.room.domain.port.in.ListRoomsQuery;
import org.example.meetingroomreservation.room.domain.port.out.RoomRepositoryPort;
import org.example.meetingroomreservation.shared.domain.PageResult;

/**
 * Implementacion del caso de uso de listado de salas.
 *
 * <p>Es deliberadamente delgada: toda la normalizacion de criterios ya la garantizan
 * {@code RoomFilter} y {@code ListRoomsQuery} al construirse, asi que aqui no hay logica que
 * duplicar. Existe para que el adaptador web no dependa del puerto de salida.
 */
public class ListRoomsService implements ListRooms {

    private final RoomRepositoryPort rooms;

    public ListRoomsService(RoomRepositoryPort rooms) {
        this.rooms = rooms;
    }

    @Override
    public PageResult<Room> list(ListRoomsQuery query) {
        return rooms.search(query);
    }
}
