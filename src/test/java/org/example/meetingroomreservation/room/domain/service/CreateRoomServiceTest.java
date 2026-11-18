package org.example.meetingroomreservation.room.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.example.meetingroomreservation.room.domain.exception.RoomNameAlreadyExistsException;
import org.example.meetingroomreservation.room.domain.model.Room;
import org.example.meetingroomreservation.room.domain.model.RoomStatus;
import org.example.meetingroomreservation.room.domain.port.in.CreateRoomCommand;
import org.example.meetingroomreservation.room.domain.port.out.RoomRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Caso de uso de creacion de salas con dobles escritos a mano.
 */
class CreateRoomServiceTest {

    private static final Clock RELOJ_FIJO =
            Clock.fixed(Instant.parse("2026-11-17T09:00:00Z"), ZoneOffset.UTC);

    private static final class FakeRoomRepository implements RoomRepositoryPort {
        private final List<String> nombresNormalizadosExistentes = new ArrayList<>();
        private final List<Room> guardadas = new ArrayList<>();
        private String ultimoNombreConsultado;

        @Override
        public boolean existsByNormalizedName(String normalizedName) {
            this.ultimoNombreConsultado = normalizedName;
            return nombresNormalizadosExistentes.contains(normalizedName);
        }

        @Override
        public Room save(Room room) {
            guardadas.add(room);
            return room;
        }
    }

    private final FakeRoomRepository repository = new FakeRoomRepository();
    private final CreateRoomService service = new CreateRoomService(repository, RELOJ_FIJO);

    @Test
    @DisplayName("crea la sala activa con el instante del reloj inyectado")
    void creaLaSala() {
        Room creada = service.create(new CreateRoomCommand("Sala Azul", 12));

        assertThat(creada.name()).isEqualTo("Sala Azul");
        assertThat(creada.capacity()).isEqualTo(12);
        assertThat(creada.status()).isEqualTo(RoomStatus.ACTIVE);
        assertThat(creada.createdAt()).isEqualTo(OffsetDateTime.parse("2026-11-17T09:00:00Z"));
        assertThat(repository.guardadas).containsExactly(creada);
    }

    @Test
    @DisplayName("la unicidad se comprueba sobre el nombre normalizado")
    void compruebaUnicidadNormalizada() {
        service.create(new CreateRoomCommand("  SALA Azul  ", 12));

        assertThat(repository.ultimoNombreConsultado).isEqualTo("sala azul");
    }

    @Test
    @DisplayName("guarda el nombre con su capitalizacion original, no normalizado")
    void guardaElNombreOriginal() {
        Room creada = service.create(new CreateRoomCommand("Sala Azul", 12));

        assertThat(creada.name()).isEqualTo("Sala Azul").isNotEqualTo("sala azul");
    }

    @Test
    @DisplayName("falla con 409 de dominio si el nombre existe en otra capitalizacion")
    void rechazaNombreDuplicado() {
        repository.nombresNormalizadosExistentes.add("sala azul");

        assertThatThrownBy(() -> service.create(new CreateRoomCommand("SALA AZUL", 12)))
                .isInstanceOf(RoomNameAlreadyExistsException.class);

        assertThat(repository.guardadas).isEmpty();
    }
}
