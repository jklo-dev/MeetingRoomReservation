package org.example.meetingroomreservation.room.infrastructure.adapter.in.web;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.example.meetingroomreservation.room.domain.model.Room;
import org.example.meetingroomreservation.shared.config.OpenApiConfig;
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
@Tag(name = "Salas", description = "Inventario de salas de reuniones")
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
    @Operation(
            summary = "Listar salas",
            description = """
                    Publico, sin autenticacion. Todos los filtros son opcionales y se aplican de
                    forma acumulativa.

                    La busqueda por `name` es **por contenido** y no distingue mayusculas: `azul`
                    encuentra `Sala Azul`. Los caracteres `%` y `_` se tratan como texto literal.

                    El orden por nombre se aplica sobre el nombre en minusculas, para que no
                    dependa de la configuracion regional de la base de datos, y lleva desempate
                    por identificador para que la paginacion sea estable.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Pagina de salas; array vacio si no hay coincidencias"),
            @ApiResponse(responseCode = "400",
                    description = """
                            Parametro invalido. `validation-error` con el array `errors`
                            indicando el campo.
                            """,
                    content = @Content)
    })
    @GetMapping
    PageResponse<RoomResponse> list(
            @Parameter(description = "Capacidad minima, inclusive. Entre 1 y 1000")
            @RequestParam(required = false) Integer minCapacity,
            @Parameter(description = "ACTIVE, INACTIVE o ALL. No distingue mayusculas")
            @RequestParam(required = false) String status,
            @Parameter(description = "Texto que debe contener el nombre")
            @RequestParam(required = false) String name,
            @Parameter(description = "name o capacity")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "asc o desc")
            @RequestParam(required = false) String direction,
            @Parameter(description = "Pagina, desde 0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Elementos por pagina. Sin maximo")
            @RequestParam(required = false) Integer size) {

        ListRoomsParams params = new ListRoomsParams(
                minCapacity, status, name, sortBy, direction, page, size);

        PageResult<Room> result = listRooms.list(params.toQuery());

        return PageResponse.from(result, RoomResponse::fromDomain);
    }

    @Operation(
            summary = "Crear una sala",
            description = """
                    Requiere rol `ADMIN`. Toda sala nace con estado `ACTIVE`; el estado no se
                    acepta en el cuerpo.

                    El nombre se guarda con su capitalizacion original pero la unicidad **no
                    distingue mayusculas**: `Sala Azul` y `sala azul` entran en conflicto.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sala creada"),
            @ApiResponse(responseCode = "400",
                    description = """
                            Campos invalidos. `validation-error`. La capacidad debe ser un entero
                            entre 1 y 1000, ambos incluidos.
                            """,
                    content = @Content),
            @ApiResponse(responseCode = "401",
                    description = "Token ausente, caducado o invalido. `authentication-required`",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "El token no tiene rol ADMIN. `insufficient-role`",
                    content = @Content),
            @ApiResponse(responseCode = "409",
                    description = """
                            Ya existe una sala con ese nombre, comparado sin distinguir
                            mayusculas. `room-name-already-exists`
                            """,
                    content = @Content)
    })
    @PostMapping
    ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
        Room created = createRoom.create(
                new CreateRoomCommand(request.name(), request.capacity()));

        return ResponseEntity
                .created(URI.create("/v1/rooms/" + created.id()))
                .body(RoomResponse.fromDomain(created));
    }
}
