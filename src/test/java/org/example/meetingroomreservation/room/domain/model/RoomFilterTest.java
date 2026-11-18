package org.example.meetingroomreservation.room.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.example.meetingroomreservation.room.domain.port.in.ListRoomsQuery;
import org.example.meetingroomreservation.shared.domain.SortDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Normalizacion de criterios. Se valida en la construccion del filtro, no en el controlador, de
 * modo que cualquier via de entrada futura aplique las mismas reglas.
 */
class RoomFilterTest {

    @Test
    @DisplayName("el estado por defecto es ACTIVE")
    void estadoPorDefecto() {
        assertThat(RoomFilter.none().status()).isEqualTo(RoomStatusFilter.ACTIVE);
        assertThat(new RoomFilter(null, null, null).status()).isEqualTo(RoomStatusFilter.ACTIVE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("un nombre en blanco se trata como si no se hubiera enviado")
    void nombreEnBlancoSeIgnora(String nombre) {
        RoomFilter filter = new RoomFilter(null, null, nombre);

        assertThat(filter.nameContainsValue()).isEmpty();
    }

    @Test
    @DisplayName("el nombre se recorta")
    void recortaElNombre() {
        RoomFilter filter = new RoomFilter(null, null, "  azul  ");

        assertThat(filter.nameContainsValue()).contains("azul");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 1001})
    @DisplayName("rechaza capacidades minimas fuera de rango")
    void rechazaCapacidadFueraDeRango(int capacidad) {
        assertThatThrownBy(() -> new RoomFilter(capacidad, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 1000})
    @DisplayName("acepta los extremos de capacidad minima")
    void aceptaExtremos(int capacidad) {
        assertThat(new RoomFilter(capacidad, null, null).minCapacityValue()).contains(capacidad);
    }

    @Test
    @DisplayName("ALL no aplica filtro de estado y no corresponde a un estado concreto")
    void todosNoAplicaFiltro() {
        assertThat(RoomStatusFilter.ALL.appliesFilter()).isFalse();
        assertThat(RoomStatusFilter.ACTIVE.appliesFilter()).isTrue();

        assertThatThrownBy(RoomStatusFilter.ALL::asStatus)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("ACTIVE e INACTIVE se traducen a su estado de sala")
    void traduceAEstadoDeSala() {
        assertThat(RoomStatusFilter.ACTIVE.asStatus()).isEqualTo(RoomStatus.ACTIVE);
        assertThat(RoomStatusFilter.INACTIVE.asStatus()).isEqualTo(RoomStatus.INACTIVE);
    }

    @Test
    @DisplayName("la consulta aplica todos los valores por defecto cuando no se indica nada")
    void consultaConValoresPorDefecto() {
        ListRoomsQuery query = new ListRoomsQuery(null, null, null, null);

        assertThat(query.filter().status()).isEqualTo(RoomStatusFilter.ACTIVE);
        assertThat(query.sortBy()).isEqualTo(RoomSortField.NAME);
        assertThat(query.direction()).isEqualTo(SortDirection.ASC);
        assertThat(query.page().page()).isZero();
        assertThat(query.page().size()).isEqualTo(20);
    }
}
