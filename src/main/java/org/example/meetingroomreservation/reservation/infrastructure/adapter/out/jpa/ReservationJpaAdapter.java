package org.example.meetingroomreservation.reservation.infrastructure.adapter.out.jpa;

import java.util.UUID;

import org.example.meetingroomreservation.reservation.domain.exception.ReservationExceptions;
import org.example.meetingroomreservation.reservation.domain.model.Reservation;
import org.example.meetingroomreservation.reservation.domain.model.ReservationStatus;
import org.example.meetingroomreservation.reservation.domain.model.TimeRange;
import org.example.meetingroomreservation.reservation.domain.port.out.ReservationRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Adaptador de persistencia que satisface {@link ReservationRepositoryPort}.
 */
@Component
class ReservationJpaAdapter implements ReservationRepositoryPort {

    /** SQLSTATE de violacion de restriccion de exclusion en PostgreSQL. */
    private static final String EXCLUSION_VIOLATION = "23P01";

    private final ReservationJpaRepository repository;

    ReservationJpaAdapter(ReservationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsActiveOverlapping(UUID roomId, TimeRange period) {
        return repository.existsOverlapping(roomId, ReservationStatus.ACTIVE,
                period.start(), period.end());
    }

    @Override
    public Reservation save(Reservation reservation) {
        try {
            // flush explicito: sin el, la violacion de la restriccion afloraria al confirmar la
            // transaccion, fuera de este try, y se escaparia sin traducir.
            ReservationJpaEntity saved =
                    repository.saveAndFlush(ReservationJpaEntity.fromDomain(reservation));
            return saved.toDomain();
        } catch (DataIntegrityViolationException e) {
            if (isExclusionViolation(e)) {
                throw new ReservationExceptions.Overlap();
            }
            throw e;
        }
    }

    /**
     * Distingue el solapamiento de cualquier otra violacion de integridad, como una clave ajena
     * invalida: traducirlas todas a 409 de solapamiento mentiria sobre la causa.
     */
    private boolean isExclusionViolation(DataIntegrityViolationException e) {
        Throwable causa = e;
        while (causa != null) {
            if (causa instanceof java.sql.SQLException sql
                    && EXCLUSION_VIOLATION.equals(sql.getSQLState())) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }
}
