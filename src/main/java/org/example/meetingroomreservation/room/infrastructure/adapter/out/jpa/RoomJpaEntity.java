package org.example.meetingroomreservation.room.infrastructure.adapter.out.jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.example.meetingroomreservation.room.domain.model.Room;
import org.example.meetingroomreservation.room.domain.model.RoomStatus;

/**
 * Entidad de persistencia de sala. Artefacto de infraestructura: no se expone en la API ni se
 * usa como modelo de dominio.
 */
@Entity
@Table(name = "rooms")
class RoomJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RoomStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected RoomJpaEntity() {
        // Requerido por JPA.
    }

    static RoomJpaEntity fromDomain(Room room) {
        RoomJpaEntity entity = new RoomJpaEntity();
        entity.id = room.id();
        entity.name = room.name();
        entity.capacity = room.capacity();
        entity.status = room.status();
        entity.createdAt = room.createdAt();
        return entity;
    }

    Room toDomain() {
        return Room.restore(id, name, capacity, status, createdAt);
    }
}
