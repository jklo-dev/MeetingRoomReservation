package org.example.meetingroomreservation.reservation.infrastructure.adapter.in.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de la peticion de creacion de reserva.
 *
 * <p>El responsable no se acepta aqui: se toma del claim {@code sub} del token. Si llega un
 * {@code ownerId}, Jackson lo ignora.
 *
 * <p>Las fechas exigen **offset obligatorio**: deserializar a {@link OffsetDateTime} rechaza
 * {@code 2026-11-20T10:00:00} sin zona. Aceptarlo obligaria a inventar una zona por defecto,
 * justo lo que se evito al elegir instantes.
 */
record CreateReservationRequest(

        @NotNull(message = "es obligatorio")
        UUID roomId,

        @NotNull(message = "es obligatorio")
        OffsetDateTime startsAt,

        @NotNull(message = "es obligatorio")
        OffsetDateTime endsAt,

        @NotNull(message = "es obligatorio")
        @Min(value = 1, message = "debe ser un entero mayor o igual a 1")
        Integer attendees) {
}
