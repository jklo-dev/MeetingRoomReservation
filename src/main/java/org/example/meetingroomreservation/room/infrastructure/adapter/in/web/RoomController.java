package org.example.meetingroomreservation.room.infrastructure.adapter.in.web;

import java.net.URI;

import jakarta.validation.Valid;

import org.example.meetingroomreservation.room.domain.model.Room;
import org.example.meetingroomreservation.room.domain.port.in.CreateRoom;
import org.example.meetingroomreservation.room.domain.port.in.CreateRoomCommand;
import org.example.meetingroomreservation.room.domain.port.in.ListRooms;
import org.example.meetingroomreservation.shared.domain.PageResult;
import org.example.meetingroomreservation.shared.web.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final ListRooms listRooms;

    RoomController(CreateRoom createRoom, ListRooms listRooms) {
        this.createRoom = createRoom;
        this.listRooms = listRooms;
    }

    /**
     * Listado publico. Por el requisito 5 de la prueba, los endpoints de consulta no requieren
     * autenticacion; un token presente se ignora.
     */
    @GetMapping
    PageResponse<RoomResponse> list(
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        ListRoomsParams params = new ListRoomsParams(
                minCapacity, status, name, sortBy, direction, page, size);

        PageResult<Room> result = listRooms.list(params.toQuery());

        return PageResponse.from(result, RoomResponse::fromDomain);
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
