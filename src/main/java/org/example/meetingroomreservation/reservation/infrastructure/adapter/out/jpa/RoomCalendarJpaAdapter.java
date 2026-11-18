package org.example.meetingroomreservation.reservation.infrastructure.adapter.out.jpa;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.example.meetingroomreservation.reservation.domain.exception.ReservationExceptions;
import org.example.meetingroomreservation.reservation.domain.model.RoomSnapshot;
import org.example.meetingroomreservation.reservation.domain.port.out.RoomCalendarPort;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Component;

/**
 * Toma el calendario de una sala en exclusiva mediante un cerrojo de fila.
 *
 * <p>Usa una consulta nativa en lugar de una entidad JPA a proposito: la tabla {@code rooms}
 * pertenece al modulo de salas, y mapearla aqui como segunda entidad duplicaria el modelo. Lo
 * unico que este modulo necesita son cuatro columnas y el cerrojo.
 */
@Component
class RoomCalendarJpaAdapter implements RoomCalendarPort {

    /**
     * Tiempo maximo de espera del cerrojo. Sin limite, una transaccion atascada dejaria la
     * peticion colgada indefinidamente.
     */
    private static final String LOCK_TIMEOUT = "30s";

    private final EntityManager entityManager;

    RoomCalendarJpaAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<RoomSnapshot> lockAndGet(UUID roomId) {
        // SET LOCAL aplica solo a la transaccion en curso, asi que no afecta a otras peticiones
        // que compartan la conexion del pool.
        entityManager.createNativeQuery("SET LOCAL lock_timeout = '" + LOCK_TIMEOUT + "'")
                .executeUpdate();

        try {
            // FOR UPDATE es lo que convierte la fila de la sala en el cerrojo de su calendario.
            List<Object[]> filas = entityManager.createNativeQuery("""
                            SELECT id, name, capacity, status
                            FROM rooms
                            WHERE id = ?1
                            FOR UPDATE
                            """)
                    .setParameter(1, roomId)
                    .getResultList();

            if (filas.isEmpty()) {
                return Optional.empty();
            }

            Object[] fila = filas.get(0);
            return Optional.of(new RoomSnapshot(
                    (UUID) fila[0],
                    (String) fila[1],
                    ((Number) fila[2]).intValue(),
                    "ACTIVE".equals(fila[3])));

        } catch (RuntimeException e) {
            // PostgreSQL aborta con lock_not_available (55P03) al agotarse lock_timeout.
            // PessimisticLockingFailureException cubre tambien CannotAcquireLockException, que
            // es su subclase y es la que Spring suele lanzar en este caso.
            if (e instanceof PessimisticLockingFailureException
                    || e instanceof QueryTimeoutException
                    || hasSqlState(e, "55P03")) {
                throw new ReservationExceptions.RoomBusy(e);
            }
            throw e;
        }
    }

    private boolean hasSqlState(Throwable error, String sqlState) {
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof SQLException sqlException
                    && sqlState.equals(sqlException.getSQLState())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
