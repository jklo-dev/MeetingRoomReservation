package org.example.meetingroomreservation.reservation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Regla de solapamiento en semantica semiabierta. Es el nucleo de la funcionalidad y se prueba
 * sin base de datos ni contexto de Spring.
 */
class TimeRangeTest {

    private static TimeRange rango(String desde, String hasta) {
        return new TimeRange(
                OffsetDateTime.parse("2026-11-20T" + desde + ":00+01:00"),
                OffsetDateTime.parse("2026-11-20T" + hasta + ":00+01:00"));
    }

    @Nested
    @DisplayName("solapamiento")
    class Solapamiento {

        private final TimeRange referencia = rango("10:00", "12:00");

        @Test
        @DisplayName("identicos se solapan")
        void identicos() {
            assertThat(referencia.overlaps(rango("10:00", "12:00"))).isTrue();
        }

        @Test
        @DisplayName("parcial por delante se solapa")
        void parcialPorDelante() {
            assertThat(referencia.overlaps(rango("09:00", "11:00"))).isTrue();
        }

        @Test
        @DisplayName("parcial por detras se solapa")
        void parcialPorDetras() {
            assertThat(referencia.overlaps(rango("11:00", "13:00"))).isTrue();
        }

        @Test
        @DisplayName("contenido se solapa")
        void contenido() {
            assertThat(referencia.overlaps(rango("10:30", "11:30"))).isTrue();
        }

        @Test
        @DisplayName("continente se solapa")
        void continente() {
            assertThat(referencia.overlaps(rango("09:00", "13:00"))).isTrue();
        }

        @Test
        @DisplayName("contiguo por delante NO se solapa: reuniones consecutivas son validas")
        void contiguoPorDelante() {
            // 09:00-10:00 termina justo cuando empieza 10:00-12:00.
            assertThat(referencia.overlaps(rango("09:00", "10:00"))).isFalse();
        }

        @Test
        @DisplayName("contiguo por detras NO se solapa")
        void contiguoPorDetras() {
            assertThat(referencia.overlaps(rango("12:00", "13:00"))).isFalse();
        }

        @Test
        @DisplayName("enteramente anterior NO se solapa")
        void anterior() {
            assertThat(referencia.overlaps(rango("08:00", "09:00"))).isFalse();
        }

        @Test
        @DisplayName("enteramente posterior NO se solapa")
        void posterior() {
            assertThat(referencia.overlaps(rango("13:00", "14:00"))).isFalse();
        }

        @Test
        @DisplayName("el solapamiento es simetrico")
        void simetrico() {
            TimeRange otro = rango("11:00", "13:00");

            assertThat(referencia.overlaps(otro)).isEqualTo(otro.overlaps(referencia));
        }
    }

    @Nested
    @DisplayName("invariantes")
    class Invariantes {

        @Test
        @DisplayName("rechaza fin igual al inicio")
        void finIgualAlInicio() {
            assertThatThrownBy(() -> rango("10:00", "10:00"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rechaza fin anterior al inicio")
        void finAnteriorAlInicio() {
            assertThatThrownBy(() -> rango("11:00", "10:00"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("calcula la duracion")
        void calculaDuracion() {
            assertThat(rango("10:00", "12:00").duration()).isEqualTo(Duration.ofHours(2));
        }
    }

    @Nested
    @DisplayName("fin futuro")
    class FinFuturo {

        @Test
        @DisplayName("ha terminado cuando el fin es anterior al instante dado")
        void yaTermino() {
            OffsetDateTime ahora = OffsetDateTime.parse("2026-11-20T13:00:00+01:00");

            assertThat(rango("10:00", "12:00").hasEndedAt(ahora)).isTrue();
        }

        @Test
        @DisplayName("ha terminado cuando el fin coincide exactamente con el instante dado")
        void finExacto() {
            // El limite es estricto: fin igual a ahora cuenta como terminada.
            OffsetDateTime ahora = OffsetDateTime.parse("2026-11-20T12:00:00+01:00");

            assertThat(rango("10:00", "12:00").hasEndedAt(ahora)).isTrue();
        }

        @Test
        @DisplayName("no ha terminado si el fin es posterior, aunque el inicio ya haya pasado")
        void enCurso() {
            // Este es el caso que la regla permite: ocupar una sala libre ahora mismo aunque la
            // reunion ya haya empezado.
            OffsetDateTime ahora = OffsetDateTime.parse("2026-11-20T11:00:00+01:00");

            assertThat(rango("10:00", "12:00").hasEndedAt(ahora)).isFalse();
        }

        @Test
        @DisplayName("los offsets distintos se comparan como instantes")
        void offsetsDistintos() {
            // 10:00+01:00 y 09:00Z son el mismo instante.
            TimeRange conOffsets = new TimeRange(
                    OffsetDateTime.parse("2026-11-20T10:00:00+01:00"),
                    OffsetDateTime.parse("2026-11-20T11:00:00Z"));

            assertThat(conOffsets.duration()).isEqualTo(Duration.ofHours(2));
        }
    }
}
