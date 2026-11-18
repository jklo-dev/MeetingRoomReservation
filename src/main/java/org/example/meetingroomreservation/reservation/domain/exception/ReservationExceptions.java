package org.example.meetingroomreservation.reservation.domain.exception;

import java.time.Duration;

/**
 * Excepciones de dominio de reservas, agrupadas por cohesion.
 *
 * <p>Cada una corresponde a **una** causa distinta del catalogo de tipos de error, de modo que
 * el manejador global pueda asignarle su propio {@code type}. Dos causas con el mismo codigo
 * HTTP siguen necesitando excepciones distintas: el solapamiento y la sala inactiva son ambos
 * 409 pero no son el mismo problema.
 */
public final class ReservationExceptions {

    private ReservationExceptions() {
    }

    /** 401: el token no identifica a un usuario valido de la aplicacion. */
    public static class InvalidAuthenticatedUser extends RuntimeException {
        public InvalidAuthenticatedUser() {
            super("Se requiere un usuario autenticado valido");
        }
    }

    /** 400: el fin no es posterior al inicio. */
    public static class InvalidTimeRange extends RuntimeException {
        public InvalidTimeRange() {
            super("El fin de la reserva debe ser posterior a su inicio");
        }
    }

    /**
     * 400: el fin no es posterior al momento actual.
     *
     * <p>El inicio **si** puede estar en el pasado y sin limite hacia atras: asi se puede ocupar
     * una sala que esta libre ahora mismo aunque la reunion ya haya empezado.
     */
    public static class MustEndInFuture extends RuntimeException {
        public MustEndInFuture() {
            super("El fin de la reserva debe ser posterior al momento actual");
        }
    }

    /** 400: la duracion esta fuera del rango permitido. */
    public static class DurationOutOfRange extends RuntimeException {
        public DurationOutOfRange(Duration min, Duration max) {
            super("La duracion de la reserva debe estar entre " + min.toMinutes()
                    + " minutos y " + max.toHours() + " horas");
        }
    }

    /**
     * 400: los asistentes superan la capacidad de la sala.
     *
     * <p>El mensaje incluye la capacidad real porque ya es informacion publica a traves del
     * listado de salas, y evita al cliente una consulta adicional para entender el rechazo.
     */
    public static class AttendeesExceedCapacity extends RuntimeException {
        public AttendeesExceedCapacity(int attendees, int capacity) {
            super("Los " + attendees + " asistentes indicados superan la capacidad de la sala, "
                    + "que es " + capacity);
        }
    }

    /** 404: el identificador de sala no corresponde a ninguna sala. */
    public static class RoomNotFound extends RuntimeException {
        public RoomNotFound() {
            super("La sala indicada no existe");
        }
    }

    /** 409: la sala existe pero esta inactiva. */
    public static class RoomNotAvailable extends RuntimeException {
        public RoomNotAvailable() {
            super("La sala indicada no esta disponible para reservas");
        }
    }

    /**
     * 409: el horario ya esta ocupado en esa sala.
     *
     * <p>No identifica la reserva en conflicto ni a su responsable: el listado publico ya ofrece
     * esa informacion a quien la busque, pero no hace falta empujarla en un error.
     */
    public static class Overlap extends RuntimeException {
        public Overlap() {
            super("La sala ya tiene una reserva en el horario indicado");
        }
    }

    /**
     * 503: no se pudo obtener el cerrojo de la sala en el tiempo maximo de espera.
     *
     * <p>Es un fallo transitorio, de ahi el 503 y no un 409: la peticion es valida y reintentarla
     * tiene sentido.
     */
    public static class RoomBusy extends RuntimeException {
        public RoomBusy(Throwable cause) {
            super("La sala esta siendo reservada por otra peticion; intentelo de nuevo", cause);
        }
    }
}
