package org.example.meetingroomreservation.reservation.infrastructure.adapter.out.jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.example.meetingroomreservation.reservation.domain.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, UUID> {

    /**
     * Solapamiento en semantica semiabierta: {@code inicio < :fin AND fin > :inicio}. Es la misma
     * condicion que expresa la restriccion de exclusion con {@code tstzrange(..., '[)')}, y debe
     * mantenerse identica a ella para que la comprobacion previa y la garantia no discrepen.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM ReservationJpaEntity r
            WHERE r.roomId = :roomId
              AND r.status = :status
              AND r.startsAt < :end
              AND r.endsAt > :start
            """)
    boolean existsOverlapping(@Param("roomId") UUID roomId,
                              @Param("status") ReservationStatus status,
                              @Param("start") OffsetDateTime start,
                              @Param("end") OffsetDateTime end);
}
