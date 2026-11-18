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
import org.example.meetingroomreservation.reservation.domain.model.RoomSnapshot;
import org.example.meetingroomreservation.reservation.domain.model.TimeRange;
import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservation;
import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservationCommand;
import org.example.meetingroomreservation.reservation.domain.port.out.OwnerLookupPort;
import org.example.meetingroomreservation.reservation.domain.port.out.ReservationRepositoryPort;
import org.example.meetingroomreservation.reservation.domain.port.out.RoomCalendarPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Caso de uso de creacion de reservas con dobles escritos a mano.
 *
 * <p>Verifica tambien el **orden de evaluacion** de la seccion 6 de la especificacion, que es lo
 * que determina que error gana cuando fallan varias condiciones a la vez.
 */
class CreateReservationServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-20T09:00:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);

    private static final UUID SALA = UUID.fromString("8c1f4e2a-7b93-4d16-a5e8-0f3c9d2b6514");
    private static final UUID USUARIO = UUID.fromString("3f2b9c7a-5d41-4e8b-9a0c-7e1d2f6b8a34");

    private static final ReservationOwner ANA =
            new ReservationOwner(USUARIO, "Ana", "Lopez");

    private static OffsetDateTime hora(String hhmm) {
        return OffsetDateTime.parse("2026-11-20T" + hhmm + ":00Z");
    }

    private static final class FakeRoomCalendar implements RoomCalendarPort {
        private RoomSnapshot sala = new RoomSnapshot(SALA, "Sala Azul", 10, true);
        private boolean existe = true;
        private boolean ocupada;
        private int vecesInvocado;

        @Override
        public Optional<RoomSnapshot> lockAndGet(UUID roomId) {
            vecesInvocado++;
            if (ocupada) {
                throw new ReservationExceptions.RoomBusy(new RuntimeException("timeout"));
            }
            return existe ? Optional.of(sala) : Optional.empty();
        }

        @Override
        public Optional<RoomSnapshot> findWithoutLock(UUID roomId) {
            return existe ? Optional.of(sala) : Optional.empty();
        }
    }

    private static final class FakeReservations implements ReservationRepositoryPort {
        private final List<Reservation> guardadas = new ArrayList<>();
        private boolean haySolapamiento;
        private boolean consultoSolapamiento;

        @Override
        public boolean existsActiveOverlapping(UUID roomId, TimeRange period) {
            consultoSolapamiento = true;
            return haySolapamiento;
        }

        @Override
        public Optional<Reservation> findById(UUID reservationId) {
            return guardadas.stream().filter(r -> r.id().equals(reservationId)).findFirst();
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

    private static final class FakeOwners implements OwnerLookupPort {
        @Override
        public Optional<ReservationOwner> findById(UUID ownerId) {
            return USUARIO.equals(ownerId) ? Optional.of(ANA) : Optional.empty();
        }
    }

    private final FakeRoomCalendar calendar = new FakeRoomCalendar();
    private final FakeReservations reservations = new FakeReservations();
    private final CreateReservationService service =
            new CreateReservationService(calendar, reservations, new FakeOwners(), RELOJ);

    private CreateReservationCommand comando(String desde, String hasta, int asistentes) {
        return new CreateReservationCommand(SALA, USUARIO, hora(desde), hora(hasta), asistentes);
    }

    private CreateReservationCommand comandoValido() {
        return comando("10:00", "11:00", 6);
    }

    @Test
    @DisplayName("crea la reserva activa con el responsable del comando y el reloj inyectado")
    void creaLaReserva() {
        CreateReservation.Result result = service.create(comandoValido());

        assertThat(result.reservation().status()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(result.reservation().ownerId()).isEqualTo(USUARIO);
        assertThat(result.reservation().roomId()).isEqualTo(SALA);
        assertThat(result.reservation().attendees()).isEqualTo(6);
        assertThat(result.reservation().createdAt())
                .isEqualTo(OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC));
        assertThat(result.room().name()).isEqualTo("Sala Azul");
        assertThat(result.owner()).isEqualTo(ANA);
        assertThat(reservations.guardadas).hasSize(1);
    }

    @Test
    @DisplayName("acepta un inicio en el pasado si el fin es futuro")
    void aceptaInicioEnElPasado() {
        // Ahora son las 09:00Z. La reunion empezo a las 08:30 y acaba a las 09:30.
        CreateReservation.Result result = service.create(comando("08:30", "09:30", 4));

        assertThat(result.reservation()).isNotNull();
    }

    @Test
    @DisplayName("rechaza una reserva cuyo fin ya paso")
    void rechazaReservaTerminada() {
        assertThatThrownBy(() -> service.create(comando("07:00", "08:00", 4)))
                .isInstanceOf(ReservationExceptions.MustEndInFuture.class);
    }

    @Test
    @DisplayName("rechaza un intervalo incoherente")
    void rechazaIntervaloIncoherente() {
        assertThatThrownBy(() -> service.create(comando("11:00", "10:00", 4)))
                .isInstanceOf(ReservationExceptions.InvalidTimeRange.class);
    }

    @Test
    @DisplayName("rechaza una duracion de 14 minutos")
    void rechazaDuracionCorta() {
        assertThatThrownBy(() -> service.create(comando("10:00", "10:14", 4)))
                .isInstanceOf(ReservationExceptions.DurationOutOfRange.class);
    }

    @Test
    @DisplayName("acepta los extremos de duracion: 15 minutos y 24 horas")
    void aceptaExtremosDeDuracion() {
        assertThat(service.create(comando("10:00", "10:15", 4)).reservation()).isNotNull();
        assertThat(service.create(new CreateReservationCommand(SALA, USUARIO,
                hora("10:00"), hora("10:00").plusHours(24), 4)).reservation()).isNotNull();
    }

    @Test
    @DisplayName("rechaza una duracion de 25 horas")
    void rechazaDuracionLarga() {
        assertThatThrownBy(() -> service.create(new CreateReservationCommand(SALA, USUARIO,
                hora("10:00"), hora("10:00").plusHours(25), 4)))
                .isInstanceOf(ReservationExceptions.DurationOutOfRange.class);
    }

    @Test
    @DisplayName("rechaza una sala inexistente con 404 de dominio")
    void rechazaSalaInexistente() {
        calendar.existe = false;

        assertThatThrownBy(() -> service.create(comandoValido()))
                .isInstanceOf(ReservationExceptions.RoomNotFound.class);
    }

    @Test
    @DisplayName("rechaza una sala inactiva")
    void rechazaSalaInactiva() {
        calendar.sala = new RoomSnapshot(SALA, "Sala Azul", 10, false);

        assertThatThrownBy(() -> service.create(comandoValido()))
                .isInstanceOf(ReservationExceptions.RoomNotAvailable.class);
    }

    @Test
    @DisplayName("acepta asistentes iguales a la capacidad exacta")
    void aceptaCapacidadExacta() {
        assertThat(service.create(comando("10:00", "11:00", 10)).reservation()).isNotNull();
    }

    @Test
    @DisplayName("rechaza asistentes por encima de la capacidad, con la capacidad en el mensaje")
    void rechazaExcesoDeAsistentes() {
        assertThatThrownBy(() -> service.create(comando("10:00", "11:00", 11)))
                .isInstanceOf(ReservationExceptions.AttendeesExceedCapacity.class)
                .hasMessageContaining("11")
                .hasMessageContaining("10");
    }

    @Test
    @DisplayName("rechaza el solapamiento con una reserva activa")
    void rechazaSolapamiento() {
        reservations.haySolapamiento = true;

        assertThatThrownBy(() -> service.create(comandoValido()))
                .isInstanceOf(ReservationExceptions.Overlap.class);

        assertThat(reservations.guardadas).isEmpty();
    }

    @Test
    @DisplayName("propaga el fallo de cerrojo como sala ocupada")
    void propagaFalloDeCerrojo() {
        calendar.ocupada = true;

        assertThatThrownBy(() -> service.create(comandoValido()))
                .isInstanceOf(ReservationExceptions.RoomBusy.class);
    }

    @Test
    @DisplayName("el cerrojo se toma ANTES de consultar solapamientos")
    void elCerrojoPrecedeALaConsulta() {
        // Si se consultara primero, la ventana de carrera seguiria abierta. Se verifica de forma
        // indirecta: con la sala inexistente, el cerrojo ya se intento y la consulta no.
        calendar.existe = false;

        assertThatThrownBy(() -> service.create(comandoValido()))
                .isInstanceOf(ReservationExceptions.RoomNotFound.class);

        assertThat(calendar.vecesInvocado).isEqualTo(1);
        assertThat(reservations.consultoSolapamiento).isFalse();
    }

    @Test
    @DisplayName("las validaciones puras se evaluan ANTES de tomar el cerrojo")
    void lasValidacionesPrecedenAlCerrojo() {
        // Mantener las comprobaciones que no necesitan base de datos fuera de la seccion
        // protegida reduce el tiempo que se retiene el cerrojo.
        assertThatThrownBy(() -> service.create(comando("10:00", "10:05", 4)))
                .isInstanceOf(ReservationExceptions.DurationOutOfRange.class);

        assertThat(calendar.vecesInvocado).isZero();
    }

    @Test
    @DisplayName("con sala inexistente y exceso de asistentes gana el 404")
    void elOrdenHaceGanarAl404() {
        // La capacidad no se puede comprobar sin conocer la sala, asi que el 404 precede
        // necesariamente a la validacion de asistentes.
        calendar.existe = false;

        assertThatThrownBy(() -> service.create(comando("10:00", "11:00", 9999)))
                .isInstanceOf(ReservationExceptions.RoomNotFound.class);
    }
}
