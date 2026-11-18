package org.example.meetingroomreservation.room.infrastructure.adapter.in.web;

import java.util.Locale;
import java.util.function.Function;

import org.example.meetingroomreservation.room.domain.model.RoomFilter;
import org.example.meetingroomreservation.room.domain.model.RoomSortField;
import org.example.meetingroomreservation.room.domain.model.RoomStatusFilter;
import org.example.meetingroomreservation.room.domain.port.in.ListRoomsQuery;
import org.example.meetingroomreservation.shared.domain.PageRequest;
import org.example.meetingroomreservation.shared.domain.SortDirection;
import org.example.meetingroomreservation.shared.web.InvalidQueryParameterException;

/**
 * Parametros de consulta del listado de salas, todos opcionales.
 *
 * <p>Se reciben como texto y se convierten aqui en lugar de dejar que Spring lo haga: asi un
 * valor fuera del enumerado produce un 400 con el array {@code errors} y un mensaje que enumera
 * los valores permitidos, en vez del error genérico de conversion de tipos.
 */
record ListRoomsParams(
        Integer minCapacity,
        String status,
        String name,
        String sortBy,
        String direction,
        Integer page,
        Integer size) {

    ListRoomsQuery toQuery() {
        return new ListRoomsQuery(
                new RoomFilter(minCapacity, parseStatus(), name),
                parseSortBy(),
                parseDirection(),
                parsePage());
    }

    private RoomStatusFilter parseStatus() {
        return parseEnum("status", status, RoomStatusFilter.class, RoomStatusFilter.DEFAULT,
                RoomStatusFilter::valueOf);
    }

    private RoomSortField parseSortBy() {
        return parseEnum("sortBy", sortBy, RoomSortField.class, RoomSortField.DEFAULT,
                RoomSortField::valueOf);
    }

    private SortDirection parseDirection() {
        return parseEnum("direction", direction, SortDirection.class, SortDirection.ASC,
                SortDirection::valueOf);
    }

    /**
     * Los parametros de consulta se comparan sin distinguir mayusculas: se teclean a mano en una
     * URL, al contrario que un valor de dominio en un cuerpo JSON. Difiere a proposito del
     * {@code role} de la creacion de usuarios, que si es estricto.
     */
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
