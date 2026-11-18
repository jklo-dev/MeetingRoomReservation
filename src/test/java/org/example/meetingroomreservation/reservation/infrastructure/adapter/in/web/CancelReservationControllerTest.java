package org.example.meetingroomreservation.reservation.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.example.meetingroomreservation.reservation.domain.exception.ReservationExceptions;
import org.example.meetingroomreservation.reservation.domain.model.Reservation;
import org.example.meetingroomreservation.reservation.domain.model.ReservationOwner;
import org.example.meetingroomreservation.reservation.domain.model.ReservationStatus;
import org.example.meetingroomreservation.reservation.domain.model.Requester;
import org.example.meetingroomreservation.reservation.domain.model.RoomSnapshot;
import org.example.meetingroomreservation.reservation.domain.model.TimeRange;
import org.example.meetingroomreservation.reservation.domain.port.in.CancelReservation;
import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservation;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato HTTP de la cancelacion de reservas. Sin base de datos.
 */
@WebMvcTest(ReservationController.class)
@Import({SecurityConfig.class, ProblemResponseWriter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "meetroom.security.jwt.secret=" + TestJwtFactory.SECRET)
class CancelReservationControllerTest {

    private static final UUID RESERVA = UUID.fromString("d41f8a62-9c57-4b03-ae18-2f6b9d0c3754");
    private static final UUID SALA = UUID.fromString("8c1f4e2a-7b93-4d16-a5e8-0f3c9d2b6514");
    private static final UUID PROPIETARIO =
            UUID.fromString("3f2b9c7a-5d41-4e8b-9a0c-7e1d2f6b8a34");

    private static final String RUTA = "/v1/reservations/" + RESERVA + "/cancel";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CancelReservation cancelReservation;

    /** El controlador tambien depende de la creacion; aqui no se ejercita. */
    @MockitoBean
    private CreateReservation createReservation;

    @MockitoBean
    private org.example.meetingroomreservation.reservation.domain.port.in.ListReservations listReservations;

    private void devuelveCancelada() {
        Reservation cancelada = Reservation.restore(RESERVA, SALA, PROPIETARIO,
                new TimeRange(
                        OffsetDateTime.parse("2026-11-20T14:00:00+01:00"),
                        OffsetDateTime.parse("2026-11-20T15:00:00+01:00")),
                4, ReservationStatus.CANCELLED,
                OffsetDateTime.parse("2026-11-17T09:00:00+01:00"),
                OffsetDateTime.parse("2026-11-17T09:31:05+01:00"),
                PROPIETARIO);

        given(cancelReservation.cancel(any(UUID.class), any(Requester.class)))
                .willReturn(new CreateReservation.Result(
                        cancelada,
                        new RoomSnapshot(SALA, "Sala Azul", 12, true),
                        new ReservationOwner(PROPIETARIO, "Ana", "Lopez")));
    }

    @Test
    @DisplayName("200 con status CANCELLED y cancelledAt informado")
    void cancela() throws Exception {
        devuelveCancelada();

        mockMvc.perform(post(RUTA)
                        .header("Authorization",
                                "Bearer " + TestJwtFactory.tokenFor(PROPIETARIO, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RESERVA.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").exists())
                .andExpect(jsonPath("$.room.name").value("Sala Azul"))
                .andExpect(jsonPath("$.owner.firstName").value("Ana"))
                // cancelledBy nunca se expone: solo se almacena para auditoria.
                .andExpect(jsonPath("$.cancelledBy").doesNotExist())
                .andExpect(jsonPath("$.owner.email").doesNotExist());
    }

    @Test
    @DisplayName("el solicitante y su condicion de administrador salen del token")
    void elSolicitanteSaleDelToken() throws Exception {
        devuelveCancelada();
        UUID admin = UUID.fromString("11111111-1111-1111-1111-111111111111");

        mockMvc.perform(post(RUTA)
                        .header("Authorization",
                                "Bearer " + TestJwtFactory.tokenFor(admin, "ADMIN")))
                .andExpect(status().isOk());

        ArgumentCaptor<Requester> captor = ArgumentCaptor.forClass(Requester.class);
        org.mockito.Mockito.verify(cancelReservation).cancel(eq(RESERVA), captor.capture());

        assertThat(captor.getValue().id()).isEqualTo(admin);
        assertThat(captor.getValue().admin()).isTrue();
    }

    @Test
    @DisplayName("un token de rol USER no se marca como administrador")
    void rolUsuarioNoEsAdmin() throws Exception {
        devuelveCancelada();

        mockMvc.perform(post(RUTA)
                        .header("Authorization",
                                "Bearer " + TestJwtFactory.tokenFor(PROPIETARIO, "USER")))
                .andExpect(status().isOk());

        ArgumentCaptor<Requester> captor = ArgumentCaptor.forClass(Requester.class);
        org.mockito.Mockito.verify(cancelReservation).cancel(any(UUID.class), captor.capture());

        assertThat(captor.getValue().admin()).isFalse();
    }

    @Test
    @DisplayName("el cuerpo enviado se ignora")
    void ignoraElCuerpo() throws Exception {
        devuelveCancelada();

        mockMvc.perform(post(RUTA)
                        .header("Authorization",
                                "Bearer " + TestJwtFactory.tokenFor(PROPIETARIO, "USER"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"loQueSea\": true}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("404 reservation-not-found")
    void reservaInexistente() throws Exception {
        willThrow(new ReservationExceptions.NotFound())
                .given(cancelReservation).cancel(any(UUID.class), any(Requester.class));

        mockMvc.perform(post(RUTA)
                        .header("Authorization",
                                "Bearer " + TestJwtFactory.tokenFor(PROPIETARIO, "USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:reservation-not-found"));
    }

    @Test
    @DisplayName("403 insufficient-role cuando no es el propietario ni administrador")
    void sinPermiso() throws Exception {
        willThrow(new ReservationExceptions.NotAllowedToCancel())
                .given(cancelReservation).cancel(any(UUID.class), any(Requester.class));

        mockMvc.perform(post(RUTA)
                        .header("Authorization",
                                "Bearer " + TestJwtFactory.tokenFor(UUID.randomUUID(), "USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:insufficient-role"))
                .andExpect(jsonPath("$.detail")
                        .value("Solo el responsable de la reserva o un administrador "
                                + "pueden cancelarla"));
    }

    @Test
    @DisplayName("409 reservation-already-cancelled")
    void yaCancelada() throws Exception {
        willThrow(new ReservationExceptions.AlreadyCancelled())
                .given(cancelReservation).cancel(any(UUID.class), any(Requester.class));

        mockMvc.perform(post(RUTA)
                        .header("Authorization",
                                "Bearer " + TestJwtFactory.tokenFor(PROPIETARIO, "USER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:reservation-already-cancelled"));
    }

    @Test
    @DisplayName("409 reservation-already-finished, distinguible del anterior por type")
    void yaFinalizada() throws Exception {
        willThrow(new ReservationExceptions.AlreadyFinished())
                .given(cancelReservation).cancel(any(UUID.class), any(Requester.class));

        mockMvc.perform(post(RUTA)
                        .header("Authorization",
                                "Bearer " + TestJwtFactory.tokenFor(PROPIETARIO, "USER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:reservation-already-finished"));
    }

    @Test
    @DisplayName("400 cuando el identificador de la ruta no es un UUID")
    void identificadorInvalido() throws Exception {
        mockMvc.perform(post("/v1/reservations/no-es-un-uuid/cancel")
                        .header("Authorization",
                                "Bearer " + TestJwtFactory.tokenFor(PROPIETARIO, "USER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("id"));
    }

    @Test
    @DisplayName("401 sin token")
    void sinToken() throws Exception {
        mockMvc.perform(post(RUTA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:authentication-required"));
    }
}
