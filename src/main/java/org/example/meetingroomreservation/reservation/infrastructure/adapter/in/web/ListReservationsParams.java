package org.example.meetingroomreservation.reservation.infrastructure.adapter.in.web;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

import org.example.meetingroomreservation.reservation.domain.model.ReservationFilter;
import org.example.meetingroomreservation.reservation.domain.model.ReservationStatusFilter;
import org.example.meetingroomreservation.reservation.domain.port.in.ListReservationsQuery;
import org.example.meetingroomreservation.shared.domain.PageRequest;
import org.example.meetingroomreservation.shared.domain.SortDirection;
import org.example.meetingroomreservation.shared.web.InvalidQueryParameterException;

/**
 * Parametros de consulta del listado de reservas, todos opcionales.
 *
 * <p>Se reciben como texto y se convierten aqui, igual que en el listado de salas: asi un valor
 * invalido produce un 400 con el array {@code errors} y un mensaje que enumera los valores
 * permitidos, en vez del error genérico de conversion de tipos.
 */
record ListReservationsParams(
        String roomId,
        String ownerId,
        String status,
        String from,
        String to,
        String direction,
        Integer page,
        Integer size) {

    ListReservationsQuery toQuery() {
        ReservationFilter filter;
        try {
            filter = new ReservationFilter(
                    parseUuid("roomId", roomId),
                    parseUuid("ownerId", ownerId),
                    parseStatus(),
                    parseInstant("from", from),
                    parseInstant("to", to));
        } catch (IllegalArgumentException e) {
            // Unico caso: to no es posterior a from, que valida el propio filtro.
            throw new InvalidQueryParameterException("to", "debe ser posterior a from");
        }

        return new ListReservationsQuery(filter, parseDirection(), parsePage());
    }

    private UUID parseUuid(String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valor.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidQueryParameterException(campo, "debe ser un UUID valido");
        }
    }

    /**
     * Exige offset, igual que el cuerpo de la creacion: aceptar una fecha sin zona obligaria a
     * inventar una por defecto.
     */
    private OffsetDateTime parseInstant(String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidQueryParameterException(campo,
                    "debe ser una fecha ISO-8601 con offset");
        }
    }

    private ReservationStatusFilter parseStatus() {
        return parseEnum("status", status, ReservationStatusFilter.class,
                ReservationStatusFilter.DEFAULT, ReservationStatusFilter::valueOf);
    }

    private SortDirection parseDirection() {
        return parseEnum("direction", direction, SortDirection.class, SortDirection.ASC,
                SortDirection::valueOf);
    }

    /** Los parametros de consulta no distinguen mayusculas: se teclean a mano en una URL. */
    private <E extends Enum<E>> E parseEnum(String campo, String valor, Class<E> tipo, E porDefecto,
                                            Function<String, E> conversor) {
        if (valor == null || valor.isBlank()) {
            return porDefecto;
        }
        try {
            return conversor.apply(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidQueryParameterException(campo, "debe ser uno de: " + permitidos(tipo));
        }
    }

    private <E extends Enum<E>> String permitidos(Class<E> tipo) {
        StringBuilder sb = new StringBuilder();
        for (E valor : tipo.getEnumConstants()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(valor.name().toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    private PageRequest parsePage() {
        int indice = page != null ? page : 0;
        int tamano = size != null ? size : PageRequest.DEFAULT_SIZE;

        if (indice < 0) {
            throw new InvalidQueryParameterException("page", "no puede ser negativa");
        }
        if (tamano < 1) {
            throw new InvalidQueryParameterException("size", "debe ser al menos 1");
        }
        return new PageRequest(indice, tamano);
    }
}
