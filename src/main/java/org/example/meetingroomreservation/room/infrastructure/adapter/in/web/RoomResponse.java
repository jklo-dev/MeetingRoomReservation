package org.example.meetingroomreservation.room.infrastructure.adapter.in.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.example.meetingroomreservation.room.domain.model.Room;
import org.example.meetingroomreservation.room.domain.model.RoomStatus;

/**
 * Representacion publica de una sala.
 */
record RoomResponse(
        UUID id,
        String name,
        int capacity,
        RoomStatus status,
        OffsetDateTime createdAt) {

    static RoomResponse fromDomain(Room room) {
        return new RoomResponse(
                room.id(),
                room.name(),
                room.capacity(),
                room.status(),
                room.createdAt());
    }
}
