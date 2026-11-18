package org.example.meetingroomreservation.room.infrastructure.config;

import java.time.Clock;

import org.example.meetingroomreservation.room.domain.port.in.CreateRoom;
import org.example.meetingroomreservation.room.domain.port.in.ListRooms;
import org.example.meetingroomreservation.room.domain.port.out.RoomRepositoryPort;
import org.example.meetingroomreservation.room.domain.service.CreateRoomService;
import org.example.meetingroomreservation.room.domain.service.ListRoomsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado de los casos de uso de sala. Vive en infraestructura para que el dominio no tenga
 * anotaciones de Spring.
 */
@Configuration
class RoomUseCaseConfig {

    @Bean
    CreateRoom createRoom(RoomRepositoryPort rooms, Clock clock) {
        return new CreateRoomService(rooms, clock);
    }

    @Bean
    ListRooms listRooms(RoomRepositoryPort rooms) {
        return new ListRoomsService(rooms);
    }
}
