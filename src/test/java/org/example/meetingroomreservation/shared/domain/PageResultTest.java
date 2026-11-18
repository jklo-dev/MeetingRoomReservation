package org.example.meetingroomreservation.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Metadatos de paginacion. Es logica aritmetica facil de equivocar, y la comparten el listado de
 * salas y el futuro de reservas.
 */
class PageResultTest {

    @Test
    @DisplayName("sin resultados, totalPages es 0 y la pagina es primera y ultima")
    void sinResultados() {
        PageResult<String> result = new PageResult<>(List.of(), 0, 20, 0);

        assertThat(result.totalPages()).isZero();
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();
    }

    @Test
    @DisplayName("una pagina parcial cuenta como una pagina completa")
    void paginaParcial() {
        // 3 elementos con tamano 20 es una sola pagina, no cero.
        PageResult<String> result = new PageResult<>(List.of("a", "b", "c"), 0, 20, 3);

        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.last()).isTrue();
    }

    @Test
    @DisplayName("el redondeo hacia arriba es correcto cuando hay resto")
    void redondeoConResto() {
        // 21 elementos de 10 en 10 son 3 paginas, no 2.
        PageResult<String> result = new PageResult<>(List.of("a"), 0, 10, 21);

        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("division exacta no genera una pagina extra vacia")
    void divisionExacta() {
        PageResult<String> result = new PageResult<>(List.of("a"), 0, 10, 20);

        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("la ultima pagina se marca como last")
    void ultimaPagina() {
        PageResult<String> result = new PageResult<>(List.of("a"), 2, 10, 21);

        assertThat(result.first()).isFalse();
        assertThat(result.last()).isTrue();
    }

    @Test
    @DisplayName("una pagina intermedia no es ni primera ni ultima")
    void paginaIntermedia() {
        PageResult<String> result = new PageResult<>(List.of("a"), 1, 10, 30);

        assertThat(result.first()).isFalse();
        assertThat(result.last()).isFalse();
    }

    @Test
    @DisplayName("una pagina mas alla del final se considera ultima")
    void paginaFueraDeRango() {
        PageResult<String> result = new PageResult<>(List.of(), 99, 10, 21);

        assertThat(result.last()).isTrue();
    }

    @Test
    @DisplayName("el contenido es inmutable")
    void contenidoInmutable() {
        PageResult<String> result = new PageResult<>(new java.util.ArrayList<>(List.of("a")), 0,
                10, 1);

        assertThatThrownBy(() -> result.content().add("b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("PageRequest rechaza pagina negativa y tamano menor que 1")
    void pageRequestValida() {
        assertThatThrownBy(() -> new PageRequest(-1, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageRequest(0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("PageRequest no impone maximo de tamano, por decision explicita")
    void pageRequestSinMaximo() {
        assertThat(new PageRequest(0, 100_000).size()).isEqualTo(100_000);
    }
}
