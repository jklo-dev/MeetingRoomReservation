package org.example.meetingroomreservation.reservation.domain.model;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Intervalo horario de una reserva.
 *
 * <p>La semantica es **semiabierta**, {@code [inicio, fin)}: dos intervalos consecutivos, donde
 * el fin de uno coincide con el inicio del otro, **no** se solapan. Es la misma semantica que
 * expresa la restriccion de exclusion en base de datos con {@code tstzrange(..., '[)')}.
 *
 * <p>La logica de solapamiento vive aqui y no en el repositorio porque es una regla de negocio y
 * debe poder probarse sin base de datos.
 */
public record TimeRange(OffsetDateTime start, OffsetDateTime end) {

    public TimeRange {
        Objects.requireNonNull(start, "el inicio es obligatorio");
        Objects.requireNonNull(end, "el fin es obligatorio");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("el fin debe ser posterior al inicio");
        }
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    /**
     * Indica si este intervalo se solapa con otro, en semantica semiabierta.
     *
     * <p>La condicion es {@code este.inicio < otro.fin} y {@code este.fin > otro.inicio}. Basta
     * con que se toquen en algun punto interior; el contacto exacto en los extremos no cuenta.
     */
    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && end.isAfter(other.start);
    }

    /**
     * Indica si el intervalo ya ha terminado respecto al instante dado.
     *
     * <p>La regla de creacion exige que el fin sea posterior al momento actual, pero **no** que
     * el inicio lo sea: asi se puede ocupar una sala que esta libre ahora mismo aunque la reunion
     * ya haya empezado.
     */
    public boolean hasEndedAt(OffsetDateTime instant) {
        return !end.isAfter(instant);
    }
}
