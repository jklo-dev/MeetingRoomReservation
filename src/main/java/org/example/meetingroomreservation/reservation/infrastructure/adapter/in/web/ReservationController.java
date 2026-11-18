package org.example.meetingroomreservation.reservation.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.example.meetingroomreservation.reservation.domain.exception.ReservationExceptions;
import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservation;
import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservationCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    private final CreateReservation createReservation;

    ReservationController(CreateReservation createReservation) {
        this.createReservation = createReservation;
    }

    @PostMapping
    ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        // El responsable sale del claim sub del token, nunca del cuerpo. Es lo que impide
        // reservar en nombre de otra persona.
        UUID ownerId = authenticatedUserId(jwt);

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

    private UUID authenticatedUserId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ReservationExceptions.InvalidAuthenticatedUser();
        }
    }
}
