package org.example.meetingroomreservation.reservation.infrastructure.adapter.out.jpa;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.example.meetingroomreservation.reservation.domain.exception.ReservationExceptions;
import org.example.meetingroomreservation.reservation.domain.model.Reservation;
import org.example.meetingroomreservation.reservation.domain.model.ReservationDetail;
import org.example.meetingroomreservation.reservation.domain.model.ReservationOwner;
import org.example.meetingroomreservation.reservation.domain.model.ReservationStatus;
import org.example.meetingroomreservation.reservation.domain.model.RoomSnapshot;
import org.example.meetingroomreservation.reservation.domain.model.TimeRange;
import org.example.meetingroomreservation.reservation.domain.port.in.ListReservationsQuery;
import org.example.meetingroomreservation.reservation.domain.port.out.ReservationRepositoryPort;
import org.example.meetingroomreservation.shared.domain.PageResult;
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
    private final EntityManager entityManager;

    ReservationJpaAdapter(ReservationJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsActiveOverlapping(UUID roomId, TimeRange period) {
        return repository.existsOverlapping(roomId, ReservationStatus.ACTIVE,
                period.start(), period.end());
    }

    @Override
    public java.util.Optional<Reservation> findById(UUID reservationId) {
        return repository.findById(reservationId).map(ReservationJpaEntity::toDomain);
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageResult<ReservationDetail> search(ListReservationsQuery query) {
        ReservationSearchQuery sql = ReservationSearchQuery.from(query);

        var consulta = entityManager.createNativeQuery(sql.selectSql());
        sql.parameters().forEach(consulta::setParameter);
        consulta.setParameter("limit", query.page().size());
        consulta.setParameter("offset", (long) query.page().page() * query.page().size());

        List<Object[]> filas = consulta.getResultList();

        var recuento = entityManager.createNativeQuery(sql.countSql());
        sql.parameters().forEach(recuento::setParameter);
        long total = ((Number) recuento.getSingleResult()).longValue();

        return new PageResult<>(
                filas.stream().map(this::toDetail).toList(),
                query.page().page(),
                query.page().size(),
                total);
    }

    private ReservationDetail toDetail(Object[] f) {
        Reservation reservation = Reservation.restore(
                ReservationSearchQuery.toUuid(f[0]),
                ReservationSearchQuery.toUuid(f[1]),
                ReservationSearchQuery.toUuid(f[2]),
                new TimeRange(
                        ReservationSearchQuery.toOffsetDateTime(f[3]),
                        ReservationSearchQuery.toOffsetDateTime(f[4])),
                ((Number) f[5]).intValue(),
                ReservationStatus.valueOf((String) f[6]),
                ReservationSearchQuery.toOffsetDateTime(f[7]),
                ReservationSearchQuery.toOffsetDateTime(f[8]),
                ReservationSearchQuery.toUuid(f[9]));

        RoomSnapshot room = new RoomSnapshot(
                reservation.roomId(),
                (String) f[10],
                ((Number) f[11]).intValue(),
                "ACTIVE".equals(f[12]));

        ReservationOwner owner = new ReservationOwner(
                reservation.ownerId(),
                (String) f[13],
                (String) f[14]);

        return new ReservationDetail(reservation, room, owner);
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
