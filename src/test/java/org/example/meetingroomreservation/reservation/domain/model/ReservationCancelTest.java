package org.example.meetingroomreservation.reservation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.example.meetingroomreservation.reservation.domain.exception.ReservationExceptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Transicion de cancelacion y sus reglas de estado y tiempo. Sin Spring ni base de datos.
 */
class ReservationCancelTest {

    private static final UUID SALA = UUID.randomUUID();
    private static final UUID PROPIETARIO = UUID.randomUUID();
    private static final UUID ADMIN = UUID.randomUUID();

    private static OffsetDateTime hora(String hhmm) {
        return OffsetDateTime.parse("2026-11-20T" + hhmm + ":00Z");
    }

    private Reservation reservaDe(String desde, String hasta) {
        return Reservation.create(SALA, PROPIETARIO,
                new TimeRange(hora(desde), hora(hasta)), 4, hora("08:00"));
    }

    @Test
    @DisplayName("cancela una reserva futura y registra cuando y quien")
    void cancelaReservaFutura() {
        Reservation reserva = reservaDe("14:00", "15:00");
        OffsetDateTime ahora = hora("10:00");

        Reservation cancelada = reserva.cancel(ADMIN, ahora);

        assertThat(cancelada.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelada.isActive()).isFalse();
        assertThat(cancelada.cancelledAt()).contains(ahora);
        assertThat(cancelada.cancelledBy()).contains(ADMIN);
    }

    @Test
    @DisplayName("la cancelacion no muta la reserva original")
    void noMutaElOriginal() {
        Reservation reserva = reservaDe("14:00", "15:00");

        reserva.cancel(ADMIN, hora("10:00"));

        assertThat(reserva.status()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reserva.cancelledAt()).isEmpty();
    }

    @Test
    @DisplayName("una reserva EN CURSO si se puede cancelar: libera el tiempo restante")
    void cancelaReservaEnCurso() {
        // Empezo a las 10:00 y acaba a las 12:00; ahora son las 11:00.
        Reservation reserva = reservaDe("10:00", "12:00");

        Reservation cancelada = reserva.cancel(PROPIETARIO, hora("11:00"));

        assertThat(cancelada.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("rechaza cancelar una reserva ya finalizada")
    void rechazaReservaFinalizada() {
        Reservation reserva = reservaDe("10:00", "11:00");

        assertThatThrownBy(() -> reserva.cancel(PROPIETARIO, hora("12:00")))
                .isInstanceOf(ReservationExceptions.AlreadyFinished.class);
    }

    @Test
    @DisplayName("el fin exactamente igual a ahora cuenta como finalizada")
    void finExactoCuentaComoFinalizada() {
        // El limite es estricto, igual que en la regla de creacion.
        Reservation reserva = reservaDe("10:00", "11:00");

        assertThatThrownBy(() -> reserva.cancel(PROPIETARIO, hora("11:00")))
                .isInstanceOf(ReservationExceptions.AlreadyFinished.class);
    }

    @Test
    @DisplayName("rechaza cancelar una reserva ya cancelada")
    void rechazaReservaYaCancelada() {
        Reservation cancelada = reservaDe("14:00", "15:00").cancel(ADMIN, hora("10:00"));

        assertThatThrownBy(() -> cancelada.cancel(PROPIETARIO, hora("11:00")))
                .isInstanceOf(ReservationExceptions.AlreadyCancelled.class);
    }

    @Test
    @DisplayName("el estado se evalua antes que el tiempo")
    void elEstadoPrecedeAlTiempo() {
        // Finalizada Y cancelada: gana el 409 de ya cancelada.
        Reservation cancelada = reservaDe("10:00", "11:00").cancel(ADMIN, hora("10:30"));

        assertThatThrownBy(() -> cancelada.cancel(PROPIETARIO, hora("12:00")))
                .isInstanceOf(ReservationExceptions.AlreadyCancelled.class);
    }

    @Test
    @DisplayName("una reserva activa no puede restaurarse con marcas de cancelacion")
    void invarianteDeCoherenciaActiva() {
        // Misma invariante que la restriccion CHECK en base de datos.
        assertThatThrownBy(() -> Reservation.restore(UUID.randomUUID(), SALA, PROPIETARIO,
                new TimeRange(hora("10:00"), hora("11:00")), 4, ReservationStatus.ACTIVE,
                hora("08:00"), hora("09:00"), ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("una reserva cancelada no puede restaurarse sin marcas de cancelacion")
    void invarianteDeCoherenciaCancelada() {
        assertThatThrownBy(() -> Reservation.restore(UUID.randomUUID(), SALA, PROPIETARIO,
                new TimeRange(hora("10:00"), hora("11:00")), 4, ReservationStatus.CANCELLED,
                hora("08:00"), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("el propietario y un administrador pueden cancelar; otro usuario no")
    void reglaDeAutorizacion() {
        Reservation reserva = reservaDe("14:00", "15:00");
        UUID otro = UUID.randomUUID();

        assertThat(new Requester(PROPIETARIO, false).canCancel(reserva)).isTrue();
        assertThat(new Requester(ADMIN, true).canCancel(reserva)).isTrue();
        assertThat(new Requester(otro, false).canCancel(reserva)).isFalse();
    }
}
