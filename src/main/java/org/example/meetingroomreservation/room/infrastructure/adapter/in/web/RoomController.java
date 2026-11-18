package org.example.meetingroomreservation.room.infrastructure.adapter.in.web;

import java.net.URI;

import jakarta.validation.Valid;

import org.example.meetingroomreservation.room.domain.model.Room;
import org.example.meetingroomreservation.room.domain.port.in.CreateRoom;
import org.example.meetingroomreservation.room.domain.port.in.CreateRoomCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada HTTP para salas.
 *
 * <p>La exigencia del rol ADMIN esta declarada en la cadena de seguridad, no aqui, porque no
 * depende de ningun recurso concreto.
 */
@RestController
@RequestMapping("/v1/rooms")
class RoomController {

    private final CreateRoom createRoom;

    RoomController(CreateRoom createRoom) {
        this.createRoom = createRoom;
    }

    @PostMapping
    ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
        Room created = createRoom.create(
                new CreateRoomCommand(request.name(), request.capacity()));

        return ResponseEntity
                .created(URI.create("/v1/rooms/" + created.id()))
                .body(RoomResponse.fromDomain(created));
    }
}
