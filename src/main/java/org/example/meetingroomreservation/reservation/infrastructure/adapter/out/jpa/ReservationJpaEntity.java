package org.example.meetingroomreservation.reservation.infrastructure.adapter.out.jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.example.meetingroomreservation.reservation.domain.model.Reservation;
import org.example.meetingroomreservation.reservation.domain.model.ReservationStatus;
import org.example.meetingroomreservation.reservation.domain.model.TimeRange;

/**
 * Entidad de persistencia de reserva.
 *
 * <p>Guarda las claves ajenas como identificadores y no como asociaciones JPA: el modulo de
 * reservas no conoce las entidades de salas ni de usuarios, y asociarlas obligaria a importarlas.
 */
@Entity
@Table(name = "reservations")
class ReservationJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(name = "attendees", nullable = false)
    private int attendees;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    protected ReservationJpaEntity() {
        // Requerido por JPA.
    }

    static ReservationJpaEntity fromDomain(Reservation reservation) {
        ReservationJpaEntity entity = new ReservationJpaEntity();
        entity.id = reservation.id();
        entity.roomId = reservation.roomId();
        entity.ownerId = reservation.ownerId();
        entity.startsAt = reservation.period().start();
        entity.endsAt = reservation.period().end();
        entity.attendees = reservation.attendees();
        entity.status = reservation.status();
        entity.createdAt = reservation.createdAt();
        entity.cancelledAt = reservation.cancelledAt().orElse(null);
        entity.cancelledBy = reservation.cancelledBy().orElse(null);
        return entity;
    }

    Reservation toDomain() {
        return Reservation.restore(id, roomId, ownerId, new TimeRange(startsAt, endsAt),
                attendees, status, createdAt, cancelledAt, cancelledBy);
    }
}
