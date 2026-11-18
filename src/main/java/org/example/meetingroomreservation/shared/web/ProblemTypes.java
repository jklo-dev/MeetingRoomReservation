package org.example.meetingroomreservation.shared.web;

import java.net.URI;

/**
 * Catalogo de tipos de problema RFC 7807.
 *
 * <p>Cada causa de error distinta tiene su propio tipo: si el {@code detail} cambia, el
 * {@code type} tambien, porque se trata de un problema diferente. {@code about:blank} esta
 * prohibido en toda la API.
 *
 * <p>Se usa un URN y no una URL para no inventar un dominio inexistente ni prometer
 * documentacion dereferenciable que no se publica.
 */
public final class ProblemTypes {

    private static final String PREFIX = "urn:meetroom:problem:";

    public static final URI VALIDATION_ERROR = URI.create(PREFIX + "validation-error");
    public static final URI MALFORMED_REQUEST = URI.create(PREFIX + "malformed-request");
    public static final URI AUTHENTICATION_REQUIRED = URI.create(PREFIX + "authentication-required");
    public static final URI INVALID_CREDENTIALS = URI.create(PREFIX + "invalid-credentials");
    public static final URI INSUFFICIENT_ROLE = URI.create(PREFIX + "insufficient-role");
    public static final URI EMAIL_ALREADY_EXISTS = URI.create(PREFIX + "email-already-exists");
    public static final URI ROOM_NAME_ALREADY_EXISTS =
            URI.create(PREFIX + "room-name-already-exists");

    // Reservas
    public static final URI INVALID_TIME_RANGE = URI.create(PREFIX + "invalid-time-range");
    public static final URI RESERVATION_MUST_END_IN_FUTURE =
            URI.create(PREFIX + "reservation-must-end-in-future");
    public static final URI RESERVATION_DURATION_OUT_OF_RANGE =
            URI.create(PREFIX + "reservation-duration-out-of-range");
    public static final URI ATTENDEES_EXCEED_CAPACITY =
            URI.create(PREFIX + "attendees-exceed-capacity");
    public static final URI ROOM_NOT_FOUND = URI.create(PREFIX + "room-not-found");
    public static final URI ROOM_NOT_AVAILABLE = URI.create(PREFIX + "room-not-available");
    public static final URI RESERVATION_OVERLAP = URI.create(PREFIX + "reservation-overlap");
    public static final URI ROOM_BUSY = URI.create(PREFIX + "room-busy");
    public static final URI RESERVATION_NOT_FOUND =
            URI.create(PREFIX + "reservation-not-found");
    public static final URI RESERVATION_ALREADY_CANCELLED =
            URI.create(PREFIX + "reservation-already-cancelled");
    public static final URI RESERVATION_ALREADY_FINISHED =
            URI.create(PREFIX + "reservation-already-finished");

    private ProblemTypes() {
    }
}
