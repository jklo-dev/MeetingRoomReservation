package org.example.meetingroomreservation.reservation.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.example.meetingroomreservation.reservation.domain.exception.ReservationExceptions;

/**
 * Reserva de una sala.
 */
public final class Reservation {

    private final UUID id;
    private final UUID roomId;
    private final UUID ownerId;
    private final TimeRange period;
    private final int attendees;
    private final ReservationStatus status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime cancelledAt;
    private final UUID cancelledBy;

    private Reservation(UUID id, UUID roomId, UUID ownerId, TimeRange period, int attendees,
                        ReservationStatus status, OffsetDateTime createdAt,
                        OffsetDateTime cancelledAt, UUID cancelledBy) {
        this.id = id;
        this.roomId = roomId;
        this.ownerId = ownerId;
        this.period = period;
        this.attendees = attendees;
        this.status = status;
        this.createdAt = createdAt;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
    }

    /**
     * Crea una reserva nueva, generando su identificador en el dominio. Toda reserva nace activa.
     */
    public static Reservation create(UUID roomId, UUID ownerId, TimeRange period, int attendees,
                                     OffsetDateTime createdAt) {
        return restore(UUID.randomUUID(), roomId, ownerId, period, attendees,
                ReservationStatus.ACTIVE, createdAt, null, null);
    }

    public static Reservation restore(UUID id, UUID roomId, UUID ownerId, TimeRange period,
                                      int attendees, ReservationStatus status,
                                      OffsetDateTime createdAt, OffsetDateTime cancelledAt,
                                      UUID cancelledBy) {
        Objects.requireNonNull(id, "el identificador es obligatorio");
        Objects.requireNonNull(roomId, "la sala es obligatoria");
        Objects.requireNonNull(ownerId, "el responsable es obligatorio");
        Objects.requireNonNull(period, "el intervalo es obligatorio");
        Objects.requireNonNull(status, "el estado es obligatorio");
        Objects.requireNonNull(createdAt, "el instante de creacion es obligatorio");

        if (attendees < 1) {
            throw new IllegalArgumentException("el numero de asistentes debe ser al menos 1");
        }

        // Misma invariante que la restriccion de coherencia en base de datos: el estado y las
        // marcas de cancelacion no pueden divergir.
        boolean cancelada = status == ReservationStatus.CANCELLED;
        boolean conMarcas = cancelledAt != null && cancelledBy != null;
        boolean sinMarcas = cancelledAt == null && cancelledBy == null;

        if (cancelada && !conMarcas) {
            throw new IllegalArgumentException(
                    "una reserva cancelada exige saber cuando y quien la cancelo");
        }
        if (!cancelada && !sinMarcas) {
            throw new IllegalArgumentException(
                    "una reserva activa no puede tener marcas de cancelacion");
        }

        return new Reservation(id, roomId, ownerId, period, attendees, status, createdAt,
                cancelledAt, cancelledBy);
    }

    /**
     * Devuelve esta reserva cancelada.
     *
     * <p>La decision de si **puede** cancelarse vive aqui y no en el servicio: es una regla sobre
     * el estado del propio objeto y debe poder probarse sin base de datos.
     *
     * <p>Una reserva **en curso** si se puede cancelar: libera la sala para el tiempo restante.
     * Es coherente con la regla de creacion, que permite reservar una sala cuyo horario ya ha
     * empezado.
     *
     * @throws ReservationExceptions.AlreadyCancelled si ya estaba cancelada
     * @throws ReservationExceptions.AlreadyFinished  si su fin no es posterior a {@code at}
     */
    public Reservation cancel(UUID by, OffsetDateTime at) {
        Objects.requireNonNull(by, "quien cancela es obligatorio");
        Objects.requireNonNull(at, "el instante de cancelacion es obligatorio");

        if (status == ReservationStatus.CANCELLED) {
            throw new ReservationExceptions.AlreadyCancelled();
        }
        if (period.hasEndedAt(at)) {
            throw new ReservationExceptions.AlreadyFinished();
        }

        return new Reservation(id, roomId, ownerId, period, attendees,
                ReservationStatus.CANCELLED, createdAt, at, by);
    }

    public UUID id() {
        return id;
    }

    public UUID roomId() {
        return roomId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public TimeRange period() {
        return period;
    }

    public int attendees() {
        return attendees;
    }

    public ReservationStatus status() {
        return status;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    public Optional<OffsetDateTime> cancelledAt() {
        return Optional.ofNullable(cancelledAt);
    }

    /**
     * Quien ejecuto la cancelacion, que puede no ser el responsable: un administrador puede
     * cancelar la reserva de otro. **No se expone en la API**, solo se almacena para auditoria.
     */
    public Optional<UUID> cancelledBy() {
        return Optional.ofNullable(cancelledBy);
    }

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return "Reservation{id=" + id + ", roomId=" + roomId + ", status=" + status + "}";
    }
}
