package org.example.meetingroomreservation.room.infrastructure.adapter.out.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RoomJpaRepository extends JpaRepository<RoomJpaEntity, UUID>,
        JpaSpecificationExecutor<RoomJpaEntity> {

    /**
     * Compara sobre el nombre en minusculas, igual que el indice unico
     * {@code rooms_name_unique_ci}. Una derivacion por nombre de metodo no serviria: Spring Data
     * generaria una comparacion sensible a mayusculas y divergiria del indice.
     */
    @Query("SELECT COUNT(r) > 0 FROM RoomJpaEntity r WHERE LOWER(r.name) = :normalizedName")
    boolean existsByNormalizedName(@Param("normalizedName") String normalizedName);
}
