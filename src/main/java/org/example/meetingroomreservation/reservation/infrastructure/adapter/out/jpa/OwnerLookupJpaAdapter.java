package org.example.meetingroomreservation.reservation.infrastructure.adapter.out.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.example.meetingroomreservation.reservation.domain.model.ReservationOwner;
import org.example.meetingroomreservation.reservation.domain.port.out.OwnerLookupPort;
import org.springframework.stereotype.Component;

/**
 * Consulta los datos del responsable.
 *
 * <p>Consulta nativa por el mismo motivo que el adaptador del calendario: la tabla {@code users}
 * pertenece al modulo de usuarios y aqui solo hacen falta tres columnas. No se selecciona el
 * correo ni el hash: no se exponen y no hacen falta.
 */
@Component
class OwnerLookupJpaAdapter implements OwnerLookupPort {

    private final EntityManager entityManager;

    OwnerLookupJpaAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<ReservationOwner> findById(UUID ownerId) {
        List<Object[]> filas = entityManager.createNativeQuery("""
                        SELECT id, first_name, last_name
                        FROM users
                        WHERE id = ?1
                        """)
                .setParameter(1, ownerId)
                .getResultList();

        if (filas.isEmpty()) {
            return Optional.empty();
        }

        Object[] fila = filas.get(0);
        return Optional.of(new ReservationOwner(
                (UUID) fila[0],
                (String) fila[1],
                (String) fila[2]));
    }
}
