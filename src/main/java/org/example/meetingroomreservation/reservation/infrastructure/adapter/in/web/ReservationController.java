package org.example.meetingroomreservation.reservation.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.example.meetingroomreservation.reservation.domain.model.ReservationDetail;
import org.example.meetingroomreservation.shared.config.OpenApiConfig;
import org.example.meetingroomreservation.reservation.domain.model.Requester;
import org.example.meetingroomreservation.reservation.domain.port.in.CancelReservation;
import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservation;
import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservationCommand;
import org.example.meetingroomreservation.reservation.domain.port.in.ListReservations;
import org.example.meetingroomreservation.shared.domain.PageResult;
import org.example.meetingroomreservation.shared.web.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada HTTP para reservas.
 *
 * <p>Requiere autenticacion pero **ningun rol concreto**: cualquier usuario autenticado puede
 * reservar.
 */
@RestController
@RequestMapping("/v1/reservations")
@Tag(name = "Reservas", description = "Creacion, cancelacion y consulta de reservas")
class ReservationController {

    /** Claim donde viaja el rol, con valor USER o ADMIN. */
    private static final String ROLE_CLAIM = "role";
    private static final String ADMIN = "ADMIN";

    private final CreateReservation createReservation;
    private final CancelReservation cancelReservation;
    private final ListReservations listReservations;

    ReservationController(CreateReservation createReservation,
                          CancelReservation cancelReservation,
                          ListReservations listReservations) {
        this.createReservation = createReservation;
        this.cancelReservation = cancelReservation;
        this.listReservations = listReservations;
    }

    /**
     * Listado publico. Por el requisito 5 de la prueba, los endpoints de consulta no requieren
     * autenticacion.
     */
    @Operation(
            summary = "Listar reservas",
            description = """
                    Publico, sin autenticacion. Todos los filtros son opcionales y acumulativos.
                    Sin ninguno, devuelve todas las reservas activas del historial.

                    **Rango temporal**: `from` y `to` delimitan un intervalo y se devuelven las
                    reservas que **se solapan** con el, no las contenidas en el. Basta con que la
                    reserva toque el rango: una de 10:00 a 12:00 **si** aparece consultando desde
                    las 11:00, porque a esa hora la sala esta ocupada. Con `from` solo, se exige
                    que el fin de la reserva sea posterior; con `to` solo, que el inicio sea
                    anterior.

                    No hay parametro de campo de ordenacion: se ordena siempre por fecha de
                    inicio, con desempate por identificador para que la paginacion sea estable.

                    El filtro por persona es por identificador y no por nombre: en un endpoint
                    sin autenticacion, buscar por nombre lo convertiria en un buscador de
                    agendas.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Pagina de reservas; array vacio si no hay coincidencias"),
            @ApiResponse(responseCode = "400",
                    description = """
                            Parametro invalido. `validation-error` con el array `errors`. Las
                            fechas exigen offset y `to` debe ser posterior a `from`.
                            """,
                    content = @Content)
    })
    @GetMapping
    PageResponse<ReservationResponse> list(
            @Parameter(description = "Identificador de la sala")
            @RequestParam(required = false) String roomId,
            @Parameter(description = "Identificador del responsable")
            @RequestParam(required = false) String ownerId,
            @Parameter(description = "ACTIVE, CANCELLED o ALL. No distingue mayusculas")
            @RequestParam(required = false) String status,
            @Parameter(description = "Limite inferior del rango. ISO-8601 con offset")
            @RequestParam(required = false) String from,
            @Parameter(description = "Limite superior del rango. ISO-8601 con offset")
            @RequestParam(required = false) String to,
            @Parameter(description = "asc o desc, sobre la fecha de inicio")
            @RequestParam(required = false) String direction,
            @Parameter(description = "Pagina, desde 0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Elementos por pagina. Sin maximo")
            @RequestParam(required = false) Integer size) {

        ListReservationsParams params = new ListReservationsParams(
                roomId, ownerId, status, from, to, direction, page, size);

        PageResult<ReservationDetail> result = listReservations.list(params.toQuery());

        return PageResponse.from(result, detalle -> ReservationResponse.of(
                detalle.reservation(), detalle.room(), detalle.owner()));
    }

    /**
     * Cancela una reserva.
     *
     * <p>Se usa POST sobre un subrecurso de accion y no DELETE porque la fila se conserva: la
     * reserva pasa a CANCELLED y seguira apareciendo en los listados. Un DELETE prometeria un
     * borrado que no ocurre.
     *
     * <p>La cadena de seguridad solo exige autenticacion: el permiso depende de comparar el
     * usuario del token con el responsable de **esta** reserva, dato que solo se conoce tras
     * consultarla, asi que la autorizacion se resuelve en el caso de uso.
     */
    @Operation(
            summary = "Cancelar una reserva",
            description = """
                    Solo el **responsable** de la reserva o un usuario con rol `ADMIN`.

                    Se usa POST sobre un subrecurso de accion y no DELETE porque la fila se
                    conserva: la reserva pasa a `CANCELLED` y sigue apareciendo en los listados,
                    con su instante de cancelacion. Quien cancelo se almacena para auditoria pero
                    **no se expone** en la respuesta.

                    Una reserva **en curso** si se puede cancelar: libera la sala para el tiempo
                    restante. Una ya finalizada no, porque no liberaria nada.

                    No lleva cuerpo; si llega uno, se ignora.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Reserva cancelada, con `status` CANCELLED y `cancelledAt`"),
            @ApiResponse(responseCode = "400",
                    description = "El identificador de la ruta no es un UUID. `validation-error`",
                    content = @Content),
            @ApiResponse(responseCode = "401",
                    description = "Token ausente, caducado o invalido. `authentication-required`",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = """
                            `insufficient-role`. Se devuelve **incluso si** la reserva ya estaba
                            cancelada o finalizada: lo contrario permitiria usar los codigos de
                            error para sondear el estado de reservas ajenas.
                            """,
                    content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "No existe esa reserva. `reservation-not-found`",
                    content = @Content),
            @ApiResponse(responseCode = "409",
                    description = """
                            Dos causas distinguibles por `type`:
                            `reservation-already-cancelled` y `reservation-already-finished`.
                            """,
                    content = @Content)
    })
    @PostMapping("/{id}/cancel")
    ReservationResponse cancel(
            @Parameter(description = "Identificador de la reserva") @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        Requester requester = new Requester(
                UUID.fromString(jwt.getSubject()),
                ADMIN.equals(jwt.getClaimAsString(ROLE_CLAIM)));

        CreateReservation.Result result = cancelReservation.cancel(id, requester);

        return ReservationResponse.of(result.reservation(), result.room(), result.owner());
    }

    @Operation(
            summary = "Crear una reserva",
            description = """
                    Requiere autenticacion pero **ningun rol concreto**. El responsable es el
                    usuario del token: no se acepta en el cuerpo, y si llega un `ownerId` se
                    ignora.

                    **Reglas temporales**: el fin debe ser posterior al momento actual, pero el
                    inicio **si** puede estar en el pasado y sin limite hacia atras. Asi se puede
                    ocupar una sala que esta libre ahora mismo aunque la reunion ya haya
                    empezado. La duracion debe estar entre 15 minutos y 24 horas, ambos
                    incluidos.

                    **Solapamiento**: los intervalos son semiabiertos, asi que 10:00-11:00 y
                    11:00-12:00 **no** se solapan y reuniones consecutivas son validas. Las
                    reservas canceladas no bloquean el horario. La garantia ultima es una
                    restriccion de exclusion en base de datos, de modo que dos reservas activas
                    solapadas son imposibles incluso ante escrituras concurrentes.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva creada"),
            @ApiResponse(responseCode = "400",
                    description = """
                            Cinco causas distintas, cada una con su propio `type`:
                            `validation-error` por formato del cuerpo, con el array `errors`;
                            `invalid-time-range` si el fin no es posterior al inicio;
                            `reservation-must-end-in-future` si el fin ya paso;
                            `reservation-duration-out-of-range` si la duracion esta fuera de
                            rango; y `attendees-exceed-capacity` si los asistentes superan la
                            capacidad, indicando la capacidad real en el `detail`.
                            """,
                    content = @Content),
            @ApiResponse(responseCode = "401",
                    description = "Token ausente, caducado o invalido. `authentication-required`",
                    content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "La sala indicada no existe. `room-not-found`",
                    content = @Content),
            @ApiResponse(responseCode = "409",
                    description = """
                            Dos causas distinguibles por `type`: `room-not-available` si la sala
                            esta inactiva, y `reservation-overlap` si el horario ya esta ocupado.
                            """,
                    content = @Content),
            @ApiResponse(responseCode = "503",
                    description = """
                            `room-busy`. No se pudo obtener el acceso exclusivo al calendario de
                            la sala en el tiempo maximo de espera. Es transitorio: reintentar
                            tiene sentido.
                            """,
                    content = @Content)
    })
    @PostMapping
    ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        // El responsable sale del claim sub del token, nunca del cuerpo. Es lo que impide
        // reservar en nombre de otra persona.
        UUID ownerId = UUID.fromString(jwt.getSubject());

        CreateReservation.Result result = createReservation.create(new CreateReservationCommand(
                request.roomId(),
                ownerId,
                request.startsAt(),
                request.endsAt(),
                request.attendees()));

        ReservationResponse body = ReservationResponse.of(
                result.reservation(), result.room(), result.owner());

        return ResponseEntity
                .created(URI.create("/v1/reservations/" + body.id()))
                .body(body);
    }
}
