package org.example.meetingroomreservation.reservation.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

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

    private Reservation(UUID id, UUID roomId, UUID ownerId, TimeRange period, int attendees,
                        ReservationStatus status, OffsetDateTime createdAt) {
        this.id = id;
        this.roomId = roomId;
        this.ownerId = ownerId;
        this.period = period;
        this.attendees = attendees;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Crea una reserva nueva, generando su identificador en el dominio. Toda reserva nace activa.
     */
    public static Reservation create(UUID roomId, UUID ownerId, TimeRange period, int attendees,
                                     OffsetDateTime createdAt) {
        return restore(UUID.randomUUID(), roomId, ownerId, period, attendees,
                ReservationStatus.ACTIVE, createdAt);
    }

    public static Reservation restore(UUID id, UUID roomId, UUID ownerId, TimeRange period,
                                      int attendees, ReservationStatus status,
                                      OffsetDateTime createdAt) {
        Objects.requireNonNull(id, "el identificador es obligatorio");
        Objects.requireNonNull(roomId, "la sala es obligatoria");
        Objects.requireNonNull(ownerId, "el responsable es obligatorio");
        Objects.requireNonNull(period, "el intervalo es obligatorio");
        Objects.requireNonNull(status, "el estado es obligatorio");
        Objects.requireNonNull(createdAt, "el instante de creacion es obligatorio");

        if (attendees < 1) {
            throw new IllegalArgumentException("el numero de asistentes debe ser al menos 1");
        }

        return new Reservation(id, roomId, ownerId, period, attendees, status, createdAt);
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

    @Override
    public String toString() {
        return "Reservation{id=" + id + ", roomId=" + roomId + ", status=" + status + "}";
    }
}
