package org.example.meetingroomreservation.reservation.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.example.meetingroomreservation.reservation.domain.model.ReservationDetail;
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
    @GetMapping
    PageResponse<ReservationResponse> list(
            @RequestParam(required = false) String roomId,
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) Integer page,
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
    @PostMapping("/{id}/cancel")
    ReservationResponse cancel(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        Requester requester = new Requester(
                UUID.fromString(jwt.getSubject()),
                ADMIN.equals(jwt.getClaimAsString(ROLE_CLAIM)));

        CreateReservation.Result result = cancelReservation.cancel(id, requester);

        return ReservationResponse.of(result.reservation(), result.room(), result.owner());
    }

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
