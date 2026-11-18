package org.example.meetingroomreservation.room.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Invariantes del modelo de sala. Sin Spring ni base de datos.
 */
class RoomTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-17T10:00:00+01:00");

    @Test
    @DisplayName("toda sala nace activa")
    void naceActiva() {
        Room room = Room.create("Sala Azul", 12, AHORA);

        assertThat(room.status()).isEqualTo(RoomStatus.ACTIVE);
        assertThat(room.isActive()).isTrue();
    }

    @Test
    @DisplayName("el nombre se recorta pero conserva su capitalizacion")
    void recortaPeroConservaCapitalizacion() {
        Room room = Room.create("   Sala Azul   ", 12, AHORA);

        assertThat(room.name()).isEqualTo("Sala Azul");
    }

    @Test
    @DisplayName("normalizedName recorta y pasa a minusculas, igual que el indice unico")
    void normalizaParaComparar() {
        // Debe coincidir exactamente con lower(name) del indice rooms_name_unique_ci.
        assertThat(Room.normalizedName("  SALA Azul  ")).isEqualTo("sala azul");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("rechaza un nombre vacio o solo con espacios")
    void rechazaNombreVacio(String nombre) {
        assertThatThrownBy(() -> Room.create(nombre, 12, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rechaza un nombre de mas de 100 caracteres")
    void rechazaNombreDemasiadoLargo() {
        assertThatThrownBy(() -> Room.create("a".repeat(101), 12, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("acepta un nombre de exactamente 100 caracteres")
    void aceptaNombreEnElLimite() {
        assertThat(Room.create("a".repeat(100), 12, AHORA).name()).hasSize(100);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 1000})
    @DisplayName("acepta los extremos de capacidad")
    void aceptaExtremosDeCapacidad(int capacidad) {
        assertThat(Room.create("Sala", capacidad, AHORA).capacity()).isEqualTo(capacidad);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 1001})
    @DisplayName("rechaza capacidades fuera de rango")
    void rechazaCapacidadFueraDeRango(int capacidad) {
        assertThatThrownBy(() -> Room.create("Sala", capacidad, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("genera el identificador en el dominio")
    void generaIdentificador() {
        Room una = Room.create("Sala Una", 4, AHORA);
        Room otra = Room.create("Sala Otra", 4, AHORA);

        assertThat(una.id()).isNotNull().isNotEqualTo(otra.id());
    }

    @Test
    @DisplayName("una sala restaurada como INACTIVE no esta activa")
    void restauraInactiva() {
        Room room = Room.restore(java.util.UUID.randomUUID(), "Sala", 4, RoomStatus.INACTIVE,
                AHORA);

        assertThat(room.isActive()).isFalse();
    }
}
