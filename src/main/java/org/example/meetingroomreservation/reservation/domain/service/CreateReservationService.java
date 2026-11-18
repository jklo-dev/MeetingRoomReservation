package org.example.meetingroomreservation.reservation.domain.service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;

import org.example.meetingroomreservation.reservation.domain.exception.ReservationExceptions;
import org.example.meetingroomreservation.reservation.domain.model.Reservation;
import org.example.meetingroomreservation.reservation.domain.model.ReservationOwner;
import org.example.meetingroomreservation.reservation.domain.model.RoomSnapshot;
import org.example.meetingroomreservation.reservation.domain.model.TimeRange;
import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservation;
import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservationCommand;
import org.example.meetingroomreservation.reservation.domain.port.out.OwnerLookupPort;
import org.example.meetingroomreservation.reservation.domain.port.out.ReservationRepositoryPort;
import org.example.meetingroomreservation.reservation.domain.port.out.RoomCalendarPort;

/**
 * Implementacion del caso de uso de creacion de reservas.
 *
 * <p>El orden de los pasos no es arbitrario: determina que error se devuelve cuando fallan
 * varias condiciones a la vez, y esta fijado en la seccion 6 de la especificacion 005.
 */
public class CreateReservationService implements CreateReservation {

    public static final Duration MIN_DURATION = Duration.ofMinutes(15);
    public static final Duration MAX_DURATION = Duration.ofHours(24);

    private final RoomCalendarPort roomCalendar;
    private final ReservationRepositoryPort reservations;
    private final OwnerLookupPort owners;
    private final Clock clock;

    public CreateReservationService(RoomCalendarPort roomCalendar,
                                    ReservationRepositoryPort reservations,
                                    OwnerLookupPort owners,
                                    Clock clock) {
        this.roomCalendar = roomCalendar;
        this.reservations = reservations;
        this.owners = owners;
        this.clock = clock;
    }

    @Override
    public Result create(CreateReservationCommand command) {
        OffsetDateTime now = OffsetDateTime.now(clock);

        // Pasos 2 a 4: comprobaciones puras, que no necesitan base de datos. Se hacen ANTES de
        // tomar el cerrojo, para no retenerlo mientras se valida: cuanto mas corta sea la seccion
        // protegida, menos espera cualquier peticion concurrente.
        TimeRange period = buildPeriod(command);
        requireEndsInFuture(period, now);
        requireValidDuration(period);

        // Paso 5: cerrojo y lectura de la sala en una sola operacion. A partir de aqui ninguna
        // otra transaccion puede crear reservas en esta sala.
        RoomSnapshot room = roomCalendar.lockAndGet(command.roomId())
                .orElseThrow(ReservationExceptions.RoomNotFound::new);

        if (!room.active()) {
            throw new ReservationExceptions.RoomNotAvailable();
        }
        if (command.attendees() > room.capacity()) {
            throw new ReservationExceptions.AttendeesExceedCapacity(
                    command.attendees(), room.capacity());
        }

        // Paso 6: protegido por el cerrojo del paso 5.
        if (reservations.existsActiveOverlapping(room.id(), period)) {
            throw new ReservationExceptions.Overlap();
        }

        ReservationOwner owner = owners.findById(command.ownerId())
                .orElseThrow(ReservationExceptions.InvalidAuthenticatedUser::new);

        // Paso 7: si la restriccion de exclusion la rechaza, el adaptador traduce la violacion a
        // la misma excepcion de solapamiento.
        Reservation saved = reservations.save(Reservation.create(
                room.id(), owner.id(), period, command.attendees(), now));

        return new Result(saved, room, owner);
    }

    /**
     * Paso 2. Traduce el fallo de invariante de {@link TimeRange} a la excepcion de dominio que
     * corresponde a su propio tipo de error.
     */
    private TimeRange buildPeriod(CreateReservationCommand command) {
        try {
            return new TimeRange(command.startsAt(), command.endsAt());
        } catch (IllegalArgumentException e) {
            throw new ReservationExceptions.InvalidTimeRange();
        }
    }

    /** Paso 3. */
    private void requireEndsInFuture(TimeRange period, OffsetDateTime now) {
        if (period.hasEndedAt(now)) {
            throw new ReservationExceptions.MustEndInFuture();
        }
    }

    /** Paso 4. Los extremos son validos: 15 minutos y 24 horas se aceptan. */
    private void requireValidDuration(TimeRange period) {
        Duration duration = period.duration();
        if (duration.compareTo(MIN_DURATION) < 0 || duration.compareTo(MAX_DURATION) > 0) {
            throw new ReservationExceptions.DurationOutOfRange(MIN_DURATION, MAX_DURATION);
        }
    }
}
