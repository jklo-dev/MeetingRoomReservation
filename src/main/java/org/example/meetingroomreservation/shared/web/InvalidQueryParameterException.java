package org.example.meetingroomreservation.shared.web;

/**
 * Parametro de consulta con un valor invalido.
 *
 * <p>Lleva el nombre del campo para que el manejador global pueda componer el array
 * {@code errors}, manteniendo una sola forma de error de validacion en toda la API.
 *
 * <p>Vive en {@code shared} y no en un modulo concreto porque el manejador global la traduce y
 * el futuro listado de reservas la reutilizara: lo contrario obligaria a que {@code shared}
 * dependiera de {@code room}.
 */
public class InvalidQueryParameterException extends RuntimeException {

    private final String field;

    public InvalidQueryParameterException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
