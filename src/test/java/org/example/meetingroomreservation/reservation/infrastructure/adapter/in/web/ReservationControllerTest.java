package org.example.meetingroomreservation.reservation.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.example.meetingroomreservation.reservation.domain.exception.ReservationExceptions;
import org.example.meetingroomreservation.reservation.domain.model.Reservation;
import org.example.meetingroomreservation.reservation.domain.model.ReservationOwner;
import org.example.meetingroomreservation.reservation.domain.model.RoomSnapshot;
import org.example.meetingroomreservation.reservation.domain.model.TimeRange;
import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservation;
import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservationCommand;
import org.example.meetingroomreservation.shared.config.SecurityConfig;
import org.example.meetingroomreservation.shared.web.GlobalExceptionHandler;
import org.example.meetingroomreservation.shared.web.ProblemResponseWriter;
import org.example.meetingroomreservation.support.TestJwtFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato HTTP de la creacion de reservas. Sin base de datos: el caso de uso es un doble.
 */
@WebMvcTest(ReservationController.class)
@Import({SecurityConfig.class, ProblemResponseWriter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "meetroom.security.jwt.secret=" + TestJwtFactory.SECRET)
class ReservationControllerTest {

    private static final UUID RESERVA = UUID.fromString("d41f8a62-9c57-4b03-ae18-2f6b9d0c3754");
    private static final UUID SALA = UUID.fromString("8c1f4e2a-7b93-4d16-a5e8-0f3c9d2b6514");
    private static final UUID USUARIO = UUID.fromString("3f2b9c7a-5d41-4e8b-9a0c-7e1d2f6b8a34");

    private static final String BODY_VALIDO = """
            {
              "roomId": "8c1f4e2a-7b93-4d16-a5e8-0f3c9d2b6514",
              "startsAt": "2026-11-20T10:00:00+01:00",
              "endsAt": "2026-11-20T11:00:00+01:00",
              "attendees": 6
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateReservation createReservation;

    private String tokenDeUsuario() {
        return TestJwtFactory.tokenFor(USUARIO, "USER");
    }

    private void devuelveReservaCreada() {
        Reservation reserva = Reservation.restore(RESERVA, SALA, USUARIO,
                new TimeRange(
                        OffsetDateTime.parse("2026-11-20T10:00:00+01:00"),
                        OffsetDateTime.parse("2026-11-20T11:00:00+01:00")),
                6,
                org.example.meetingroomreservation.reservation.domain.model.ReservationStatus.ACTIVE,
                OffsetDateTime.parse("2026-11-17T09:00:00+01:00"));

        given(createReservation.create(any(CreateReservationCommand.class)))
                .willReturn(new CreateReservation.Result(
                        reserva,
                        new RoomSnapshot(SALA, "Sala Azul", 12, true),
                        new ReservationOwner(USUARIO, "Ana", "Lopez")));
    }

    @Test
    @DisplayName("201 con sala y responsable anidados, capacidad incluida y sin correo")
    void creaReserva() throws Exception {
        devuelveReservaCreada();

        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v1/reservations/" + RESERVA))
                .andExpect(jsonPath("$.id").value(RESERVA.toString()))
                .andExpect(jsonPath("$.room.id").value(SALA.toString()))
                .andExpect(jsonPath("$.room.name").value("Sala Azul"))
                .andExpect(jsonPath("$.room.capacity").value(12))
                .andExpect(jsonPath("$.room.status").doesNotExist())
                .andExpect(jsonPath("$.owner.id").value(USUARIO.toString()))
                .andExpect(jsonPath("$.owner.firstName").value("Ana"))
                .andExpect(jsonPath("$.owner.lastName").value("Lopez"))
                .andExpect(jsonPath("$.owner.email").doesNotExist())
                .andExpect(jsonPath("$.attendees").value(6))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("el responsable sale del token, no del cuerpo")
    void elResponsableSaleDelToken() throws Exception {
        devuelveReservaCreada();
        UUID otro = UUID.fromString("11111111-1111-1111-1111-111111111111");

        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        // Se intenta colar un ownerId distinto en el cuerpo.
                        .content(BODY_VALIDO.replace("\"attendees\": 6",
                                "\"attendees\": 6, \"ownerId\": \"" + otro + "\"")))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateReservationCommand> captor =
                ArgumentCaptor.forClass(CreateReservationCommand.class);
        org.mockito.Mockito.verify(createReservation).create(captor.capture());

        assertThat(captor.getValue().ownerId()).isEqualTo(USUARIO).isNotEqualTo(otro);
    }

    @Test
    @DisplayName("400 validation-error con campos ausentes")
    void rechazaCamposAusentes() throws Exception {
        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:validation-error"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("400 validation-error cuando attendees no es entero")
    void rechazaAsistentesDecimales() throws Exception {
        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO.replace("\"attendees\": 6", "\"attendees\": 6.5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("attendees"));
    }

    @Test
    @DisplayName("400 con una fecha sin offset, nombrando el campo exacto")
    void rechazaFechaSinOffset() throws Exception {
        // Aceptar una fecha sin zona obligaria a inventar una por defecto, justo lo que se evito
        // al elegir instantes.
        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO.replace("2026-11-20T10:00:00+01:00",
                                "2026-11-20T10:00:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("startsAt"));
    }

    @Test
    @DisplayName("400 con un roomId que no es UUID, nombrando el campo exacto")
    void rechazaRoomIdInvalido() throws Exception {
        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO.replace(SALA.toString(), "no-es-un-uuid")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("roomId"));
    }

    @Test
    @DisplayName("400 malformed-request cuando el JSON esta roto y no hay campo que senalar")
    void rechazaJsonRoto() throws Exception {
        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:malformed-request"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("400 invalid-time-range")
    void rechazaIntervaloIncoherente() throws Exception {
        willThrow(new ReservationExceptions.InvalidTimeRange())
                .given(createReservation).create(any(CreateReservationCommand.class));

        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:invalid-time-range"))
                // Las reglas de negocio no usan el array errors: no fallan por un campo aislado.
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("400 reservation-must-end-in-future")
    void rechazaReservaTerminada() throws Exception {
        willThrow(new ReservationExceptions.MustEndInFuture())
                .given(createReservation).create(any(CreateReservationCommand.class));

        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:reservation-must-end-in-future"));
    }

    @Test
    @DisplayName("400 reservation-duration-out-of-range")
    void rechazaDuracionFueraDeRango() throws Exception {
        willThrow(new ReservationExceptions.DurationOutOfRange(
                Duration.ofMinutes(15), Duration.ofHours(24)))
                .given(createReservation).create(any(CreateReservationCommand.class));

        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:reservation-duration-out-of-range"))
                .andExpect(jsonPath("$.detail")
                        .value("La duracion de la reserva debe estar entre 15 minutos y 24 horas"));
    }

    @Test
    @DisplayName("400 attendees-exceed-capacity con la capacidad real en el mensaje")
    void rechazaExcesoDeAsistentes() throws Exception {
        willThrow(new ReservationExceptions.AttendeesExceedCapacity(6, 4))
                .given(createReservation).create(any(CreateReservationCommand.class));

        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:attendees-exceed-capacity"))
                .andExpect(jsonPath("$.detail")
                        .value("Los 6 asistentes indicados superan la capacidad de la sala, "
                                + "que es 4"));
    }

    @Test
    @DisplayName("404 room-not-found")
    void rechazaSalaInexistente() throws Exception {
        willThrow(new ReservationExceptions.RoomNotFound())
                .given(createReservation).create(any(CreateReservationCommand.class));

        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:room-not-found"));
    }

    @Test
    @DisplayName("409 room-not-available con la sala inactiva")
    void rechazaSalaInactiva() throws Exception {
        willThrow(new ReservationExceptions.RoomNotAvailable())
                .given(createReservation).create(any(CreateReservationCommand.class));

        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:room-not-available"));
    }

    @Test
    @DisplayName("409 reservation-overlap, distinguible del anterior por type")
    void rechazaSolapamiento() throws Exception {
        willThrow(new ReservationExceptions.Overlap())
                .given(createReservation).create(any(CreateReservationCommand.class));

        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:reservation-overlap"))
                .andExpect(jsonPath("$.detail")
                        .value("La sala ya tiene una reserva en el horario indicado"));
    }

    @Test
    @DisplayName("503 room-busy cuando no se obtiene el cerrojo")
    void salaOcupadaPorOtraPeticion() throws Exception {
        willThrow(new ReservationExceptions.RoomBusy(new RuntimeException("timeout")))
                .given(createReservation).create(any(CreateReservationCommand.class));

        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value("urn:meetroom:problem:room-busy"));
    }

    @Test
    @DisplayName("401 sin token")
    void rechazaSinToken() throws Exception {
        mockMvc.perform(post("/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:authentication-required"));
    }

    @Test
    @DisplayName("401 cuando el subject del token no es un UUID")
    void rechazaSubjectInvalido() throws Exception {
        String token = TestJwtFactory.tokenForSubject("no-es-un-uuid", "USER");

        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:authentication-required"));
    }

    @Test
    @DisplayName("un token de rol USER basta: no se exige ningun rol concreto")
    void noExigeRolConcreto() throws Exception {
        devuelveReservaCreada();

        mockMvc.perform(post("/v1/reservations")
                        .header("Authorization", "Bearer " + tokenDeUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isCreated());
    }
}
