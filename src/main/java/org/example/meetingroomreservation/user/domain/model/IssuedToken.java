package org.example.meetingroomreservation.user.domain.model;

/**
 * Token emitido y su vigencia.
 *
 * @param value            cadena del token firmado
 * @param expiresInSeconds segundos hasta la caducidad
 */
public record IssuedToken(String value, long expiresInSeconds) {

    /**
     * No incluye el valor del token: este objeto podria acabar en un log.
     */
    @Override
    public String toString() {
        return "IssuedToken{expiresInSeconds=" + expiresInSeconds + "}";
    }
}
