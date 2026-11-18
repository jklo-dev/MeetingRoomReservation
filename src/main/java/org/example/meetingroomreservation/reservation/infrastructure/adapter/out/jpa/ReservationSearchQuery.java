package org.example.meetingroomreservation.reservation.infrastructure.adapter.out.jpa;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.example.meetingroomreservation.reservation.domain.model.ReservationFilter;
import org.example.meetingroomreservation.reservation.domain.port.in.ListReservationsQuery;
import org.example.meetingroomreservation.shared.domain.SortDirection;

/**
 * Construye el SQL del listado de reservas.
 *
 * <p>Se compone dinamicamente anadiendo solo las condiciones de los filtros presentes, en lugar
 * de usar el patron {@code (:param IS NULL OR columna = :param)}. El motivo es practico: con
 * parametros nulos en una consulta nativa, PostgreSQL no puede inferir el tipo y falla con
 * «could not determine data type of parameter».
 *
 * <p><b>Todos los valores van como parametros vinculados</b>; lo unico dinamico es que una
 * condicion aparezca o no, asi que no hay superficie de inyeccion.
 */
final class ReservationSearchQuery {

    /**
     * Una sola consulta con los JOIN a salas y usuarios: evita el problema de las N+1 por
     * construccion, en lugar de confiar en que la carga perezosa este bien configurada.
     */
    private static final String SELECT = """
            SELECT r.id, r.room_id, r.owner_id, r.starts_at, r.ends_at, r.attendees,
                   r.status, r.created_at, r.cancelled_at, r.cancelled_by,
                   ro.name, ro.capacity, ro.status,
                   u.first_name, u.last_name
            FROM reservations r
            JOIN rooms ro ON ro.id = r.room_id
            JOIN users u ON u.id = r.owner_id
            """;

    private static final String COUNT = """
            SELECT COUNT(*)
            FROM reservations r
            JOIN rooms ro ON ro.id = r.room_id
            JOIN users u ON u.id = r.owner_id
            """;

    private final String where;
    private final Map<String, Object> parameters;
    private final SortDirection direction;

    private ReservationSearchQuery(String where, Map<String, Object> parameters,
                                   SortDirection direction) {
        this.where = where;
        this.parameters = parameters;
        this.direction = direction;
    }

    static ReservationSearchQuery from(ListReservationsQuery query) {
        ReservationFilter filter = query.filter();
        List<String> condiciones = new ArrayList<>();
        Map<String, Object> parametros = new LinkedHashMap<>();

        filter.roomIdValue().ifPresent(roomId -> {
            condiciones.add("r.room_id = :roomId");
            parametros.put("roomId", roomId);
        });

        filter.ownerIdValue().ifPresent(ownerId -> {
            condiciones.add("r.owner_id = :ownerId");
            parametros.put("ownerId", ownerId);
        });

        if (filter.status().appliesFilter()) {
            condiciones.add("r.status = :status");
            parametros.put("status", filter.status().asStatus().name());
        }

        // Solapamiento con el rango, no contencion: basta con que la reserva toque el rango.
        filter.fromValue().ifPresent(from -> {
            condiciones.add("r.ends_at > :from");
            parametros.put("from", from);
        });
        filter.toValue().ifPresent(to -> {
            condiciones.add("r.starts_at < :to");
            parametros.put("to", to);
        });

        String where = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);

        return new ReservationSearchQuery(where, parametros, query.direction());
    }

    String selectSql() {
        // El desempate por id es obligatorio: las reuniones se agendan en horas en punto, asi
        // que varias reservas con el mismo starts_at son la norma. Sin desempate, la paginacion
        // podria duplicar u omitir elementos entre paginas.
        String sentido = direction == SortDirection.ASC ? "ASC" : "DESC";
        return SELECT + where + " ORDER BY r.starts_at " + sentido + ", r.id ASC"
                + " LIMIT :limit OFFSET :offset";
    }

    String countSql() {
        return COUNT + where;
    }

    Map<String, Object> parameters() {
        return parameters;
    }

    /**
     * Convierte el valor temporal que devuelve el driver.
     *
     * <p>Segun la configuracion, una columna {@code timestamptz} puede llegar como
     * {@link OffsetDateTime}, {@link Instant} o {@link java.sql.Timestamp}. Se normaliza aqui en
     * lugar de asumir un tipo concreto.
     */
    static OffsetDateTime toOffsetDateTime(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof OffsetDateTime odt) {
            return odt;
        }
        if (valor instanceof Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        if (valor instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        if (valor instanceof java.time.LocalDateTime ldt) {
            return ldt.atOffset(ZoneOffset.UTC);
        }
        throw new IllegalStateException(
                "tipo temporal inesperado desde la base de datos: " + valor.getClass());
    }

    static UUID toUuid(Object valor) {
        if (valor == null) {
            return null;
        }
        return valor instanceof UUID uuid ? uuid : UUID.fromString(valor.toString());
    }
}
