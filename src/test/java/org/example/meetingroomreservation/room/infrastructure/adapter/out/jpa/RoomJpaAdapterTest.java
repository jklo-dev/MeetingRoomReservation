package org.example.meetingroomreservation.room.infrastructure.adapter.out.jpa;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.sql.SQLException;
import java.time.OffsetDateTime;

import org.example.meetingroomreservation.room.domain.exception.RoomNameAlreadyExistsException;
import org.example.meetingroomreservation.room.domain.model.Room;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class RoomJpaAdapterTest {

    @Mock
    private RoomJpaRepository repository;

    @Test
    void traduceLaViolacionDelIndiceDeNombre() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException(
                "error", new SQLException("constraint rooms_name_unique_ci"));
        given(repository.saveAndFlush(any(RoomJpaEntity.class))).willThrow(violation);

        RoomJpaAdapter adapter = new RoomJpaAdapter(repository);

        assertThatThrownBy(() -> adapter.save(Room.create(
                "Sala Azul", 10, OffsetDateTime.parse("2026-11-18T10:00:00Z"))))
                .isInstanceOf(RoomNameAlreadyExistsException.class);
    }

    @Test
    void conservaLasDemasViolacionesDeIntegridad() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException(
                "error", new SQLException("constraint rooms_capacity_valid"));
        given(repository.saveAndFlush(any(RoomJpaEntity.class))).willThrow(violation);

        RoomJpaAdapter adapter = new RoomJpaAdapter(repository);

        assertThatThrownBy(() -> adapter.save(Room.create(
                "Sala Azul", 10, OffsetDateTime.parse("2026-11-18T10:00:00Z"))))
                .isSameAs(violation);
    }
}
