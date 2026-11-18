package org.example.meetingroomreservation.shared.web;

/**
 * Error asociado a un campo concreto de la peticion.
 *
 * <p>Solo aparece en las respuestas de tipo {@code validation-error}. Las reglas de negocio no
 * usan este array: no fallan por un campo aislado sino por la combinacion de varios.
 */
public record FieldErrorDetail(String field, String message) {
}
