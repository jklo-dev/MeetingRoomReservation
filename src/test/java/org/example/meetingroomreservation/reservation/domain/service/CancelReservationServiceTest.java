package org.example.meetingroomreservation.reservation.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.example.meetingroomreservation.reservation.domain.exception.ReservationExceptions;
import org.example.meetingroomreservation.reservation.domain.model.Reservation;
import org.example.meetingroomreservation.reservation.domain.model.ReservationOwner;
import org.example.meetingroomreservation.reservation.domain.model.ReservationStatus;
import org.example.meetingroomreservation.reservation.domain.model.Requester;
import org.example.meetingroomreservation.reservation.domain.model.RoomSnapshot;
import org.example.meetingroomreservation.reservation.domain.model.TimeRange;
import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservation;
import org.example.meetingroomreservation.reservation.domain.port.out.OwnerLookupPort;
import org.example.meetingroomreservation.reservation.domain.port.out.ReservationRepositoryPort;
import org.example.meetingroomreservation.reservation.domain.port.out.RoomCalendarPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Caso de uso de cancelacion con dobles. Verifica el **orden de evaluacion** de la seccion 6 de
 * la especificacion 006, que es lo que impide usar los codigos de error para sondear reservas
 * ajenas.
 */
class CancelReservationServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-20T10:00:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);

    private static final UUID SALA = UUID.randomUUID();
    private static final UUID PROPIETARIO = UUID.randomUUID();
    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID EXTRANO = UUID.randomUUID();

    private static OffsetDateTime hora(String hhmm) {
        return OffsetDateTime.parse("2026-11-20T" + hhmm + ":00Z");
    }

    private static final class FakeReservations implements ReservationRepositoryPort {
        private final List<Reservation> existentes = new ArrayList<>();
        private final List<Reservation> guardadas = new ArrayList<>();

        @Override
        public boolean existsActiveOverlapping(UUID roomId, TimeRange period) {
            return false;
        }

        @Override
        public Optional<Reservation> findById(UUID reservationId) {
            return existentes.stream().filter(r -> r.id().equals(reservationId)).findFirst();
        }

        @Override
        public Reservation save(Reservation reservation) {
            guardadas.add(reservation);
            return reservation;
        }

        /** El listado no se ejercita en este test. */
        @Override
        public org.example.meetingroomreservation.shared.domain.PageResult<
                org.example.meetingroomreservation.reservation.domain.model.ReservationDetail>
                search(org.example.meetingroomreservation.reservation.domain.port.in
                        .ListReservationsQuery query) {
            throw new UnsupportedOperationException("no se usa en este test");
        }
    }

    private static final class FakeRooms implements RoomCalendarPort {
        private int vecesConCerrojo;

        @Override
        public Optional<RoomSnapshot> lockAndGet(UUID roomId) {
            vecesConCerrojo++;
            return findWithoutLock(roomId);
        }

        @Override
        public Optional<RoomSnapshot> findWithoutLock(UUID roomId) {
            return Optional.of(new RoomSnapshot(SALA, "Sala Azul", 10, true));
        }
    }

    private static final class FakeOwners implements OwnerLookupPort {
        @Override
        public Optional<ReservationOwner> findById(UUID ownerId) {
            return Optional.of(new ReservationOwner(ownerId, "Ana", "Lopez"));
        }
    }

    private final FakeReservations reservations = new FakeReservations();
    private final FakeRooms rooms = new FakeRooms();
    private final CancelReservationService service =
            new CancelReservationService(reservations, rooms, new FakeOwners(), RELOJ);

    private Reservation reservaActiva(String desde, String hasta) {
        Reservation reserva = Reservation.create(SALA, PROPIETARIO,
                new TimeRange(hora(desde), hora(hasta)), 4, hora("08:00"));
        reservations.existentes.add(reserva);
        return reserva;
    }

    @Test
    @DisplayName("el propietario cancela su reserva y se registra quien y cuando")
    void elPropietarioCancela() {
        Reservation reserva = reservaActiva("14:00", "15:00");

        CreateReservation.Result result =
                service.cancel(reserva.id(), new Requester(PROPIETARIO, false));

        assertThat(result.reservation().status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(result.reservation().cancelledBy()).contains(PROPIETARIO);
        assertThat(result.reservation().cancelledAt())
                .contains(OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC));
        assertThat(result.room().name()).isEqualTo("Sala Azul");
        assertThat(reservations.guardadas).hasSize(1);
    }

    @Test
    @DisplayName("un administrador cancela una reserva ajena y queda registrado como autor")
    void elAdminCancelaAjena() {
        Reservation reserva = reservaActiva("14:00", "15:00");

        CreateReservation.Result result =
                service.cancel(reserva.id(), new Requester(ADMIN, true));

        // cancelled_by es quien cancelo, no el responsable: esa es la informacion util.
        assertThat(result.reservation().cancelledBy()).contains(ADMIN);
        assertThat(result.reservation().ownerId()).isEqualTo(PROPIETARIO);
    }

    @Test
    @DisplayName("404 cuando la reserva no existe")
    void reservaInexistente() {
        assertThatThrownBy(() -> service.cancel(UUID.randomUUID(),
                new Requester(PROPIETARIO, false)))
                .isInstanceOf(ReservationExceptions.NotFound.class);
    }

    @Test
    @DisplayName("403 cuando quien solicita no es el propietario ni administrador")
    void extranoNoPuedeCancelar() {
        Reservation reserva = reservaActiva("14:00", "15:00");

        assertThatThrownBy(() -> service.cancel(reserva.id(), new Requester(EXTRANO, false)))
                .isInstanceOf(ReservationExceptions.NotAllowedToCancel.class);

        assertThat(reservations.guardadas).isEmpty();
    }

    @Test
    @DisplayName("la autorizacion se evalua ANTES del estado: un extrano recibe 403, no 409")
    void laAutorizacionPrecedeAlEstado() {
        // Si fuera al reves, un extrano podria distinguir "activa" de "cancelada" por el codigo
        // de error y sondear el estado de reservas ajenas.
        Reservation reserva = reservaActiva("14:00", "15:00");
        reservations.existentes.clear();
        reservations.existentes.add(reserva.cancel(ADMIN, hora("09:00")));

        assertThatThrownBy(() -> service.cancel(reserva.id(), new Requester(EXTRANO, false)))
                .isInstanceOf(ReservationExceptions.NotAllowedToCancel.class);
    }

    @Test
    @DisplayName("la autorizacion se evalua ANTES del tiempo: un extrano recibe 403, no 409")
    void laAutorizacionPrecedeAlTiempo() {
        reservaActiva("07:00", "08:00");
        UUID id = reservations.existentes.get(0).id();

        assertThatThrownBy(() -> service.cancel(id, new Requester(EXTRANO, false)))
                .isInstanceOf(ReservationExceptions.NotAllowedToCancel.class);
    }

    @Test
    @DisplayName("409 al cancelar una reserva ya cancelada")
    void yaCancelada() {
        Reservation reserva = reservaActiva("14:00", "15:00");
        reservations.existentes.clear();
        reservations.existentes.add(reserva.cancel(PROPIETARIO, hora("09:00")));

        assertThatThrownBy(() -> service.cancel(reserva.id(), new Requester(PROPIETARIO, false)))
                .isInstanceOf(ReservationExceptions.AlreadyCancelled.class);
    }

    @Test
    @DisplayName("409 al cancelar una reserva ya finalizada")
    void yaFinalizada() {
        Reservation reserva = reservaActiva("07:00", "08:00");

        assertThatThrownBy(() -> service.cancel(reserva.id(), new Requester(PROPIETARIO, false)))
                .isInstanceOf(ReservationExceptions.AlreadyFinished.class);
    }

    @Test
    @DisplayName("una reserva en curso si se puede cancelar")
    void reservaEnCurso() {
        // Ahora son las 10:00; la reunion va de 09:00 a 11:00.
        Reservation reserva = reservaActiva("09:00", "11:00");

        CreateReservation.Result result =
                service.cancel(reserva.id(), new Requester(PROPIETARIO, false));

        assertThat(result.reservation().status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("la cancelacion NO toma ningun cerrojo")
    void noTomaCerrojo() {
        // Afecta a una unica fila por clave primaria y no decide nada en funcion de la ausencia
        // de otras filas: no hay write skew posible, asi que el cerrojo seria coste sin garantia.
        Reservation reserva = reservaActiva("14:00", "15:00");

        service.cancel(reserva.id(), new Requester(PROPIETARIO, false));

        assertThat(rooms.vecesConCerrojo).isZero();
    }
}
