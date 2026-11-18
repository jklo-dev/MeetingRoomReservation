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
    public static final URI INSUFFICIENT_ROLE = URI.create(PREFIX + "insufficient-role");
    public static final URI EMAIL_ALREADY_EXISTS = URI.create(PREFIX + "email-already-exists");

    private ProblemTypes() {
    }
}
