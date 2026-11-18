package org.example.meetingroomreservation.room.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.example.meetingroomreservation.room.domain.exception.RoomNameAlreadyExistsException;
import org.example.meetingroomreservation.room.domain.model.Room;
import org.example.meetingroomreservation.room.domain.model.RoomStatus;
import org.example.meetingroomreservation.room.domain.port.in.CreateRoom;
import org.example.meetingroomreservation.room.domain.port.in.CreateRoomCommand;
import org.example.meetingroomreservation.shared.config.SecurityConfig;
import org.example.meetingroomreservation.shared.web.GlobalExceptionHandler;
import org.example.meetingroomreservation.shared.web.ProblemResponseWriter;
import org.example.meetingroomreservation.support.TestJwtFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato HTTP del endpoint de creacion de salas. Sin base de datos.
 */
@WebMvcTest(RoomController.class)
@Import({SecurityConfig.class, ProblemResponseWriter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "meetroom.security.jwt.secret=" + TestJwtFactory.SECRET)
class RoomControllerTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SALA_ID = UUID.fromString("8c1f4e2a-7b93-4d16-a5e8-0f3c9d2b6514");

    private static final String BODY_VALIDO = """
            { "name": "Sala Azul", "capacity": 12 }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateRoom createRoom;

    /** El controlador tambien depende del listado; aqui no se ejercita. */
    @MockitoBean
    private org.example.meetingroomreservation.room.domain.port.in.ListRooms listRooms;

    private String adminToken() {
        return TestJwtFactory.tokenFor(ADMIN_ID, "ADMIN");
    }

    @Test
    @DisplayName("201 con el cuerpo documentado, Location y estado ACTIVE")
    void creaSala() throws Exception {
        Room creada = Room.restore(SALA_ID, "Sala Azul", 12, RoomStatus.ACTIVE,
                OffsetDateTime.parse("2026-11-17T10:00:00+01:00"));
        given(createRoom.create(any(CreateRoomCommand.class))).willReturn(creada);

        mockMvc.perform(post("/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v1/rooms/" + SALA_ID))
                .andExpect(jsonPath("$.id").value(SALA_ID.toString()))
                .andExpect(jsonPath("$.name").value("Sala Azul"))
                .andExpect(jsonPath("$.capacity").value(12))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("el campo status enviado en el body se ignora")
    void ignoraElEstadoEnviado() throws Exception {
        Room creada = Room.restore(SALA_ID, "Sala Azul", 12, RoomStatus.ACTIVE,
                OffsetDateTime.parse("2026-11-17T10:00:00+01:00"));
        given(createRoom.create(any(CreateRoomCommand.class))).willReturn(creada);

        mockMvc.perform(post("/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Sala Azul", "capacity": 12, "status": "INACTIVE" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5, 1001})
    @DisplayName("400 validation-error con capacidad fuera de rango")
    void rechazaCapacidadFueraDeRango(int capacidad) throws Exception {
        mockMvc.perform(post("/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Sala\", \"capacity\": " + capacidad + " }"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("capacity"));
    }

    @Test
    @DisplayName("400 cuando falta la capacidad")
    void rechazaCapacidadAusente() throws Exception {
        mockMvc.perform(post("/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Sala Azul" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("capacity"))
                .andExpect(jsonPath("$.errors[0].message").value("es obligatoria"));
    }

    @Test
    @DisplayName("400 cuando el nombre esta vacio")
    void rechazaNombreVacio() throws Exception {
        mockMvc.perform(post("/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "   ", "capacity": 12 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    @DisplayName("400 cuando la capacidad no es numerica")
    void rechazaCapacidadNoNumerica() throws Exception {
        mockMvc.perform(post("/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Sala Azul", "capacity": "doce" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("capacity"));
    }

    @Test
    @DisplayName("409 room-name-already-exists cuando el nombre ya existe")
    void rechazaNombreDuplicado() throws Exception {
        willThrow(new RoomNameAlreadyExistsException())
                .given(createRoom).create(any(CreateRoomCommand.class));

        mockMvc.perform(post("/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:room-name-already-exists"))
                .andExpect(jsonPath("$.detail")
                        .value("Ya existe una sala con el nombre indicado"));
    }

    @Test
    @DisplayName("la ruta con barra final no existe y devuelve 404")
    void laBarraFinalNoCoincideConElMatcher() throws Exception {
        // Desde Spring Boot 3, /v1/rooms y /v1/rooms/ son rutas distintas.
        mockMvc.perform(post("/v1/rooms/")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("una ruta inexistente devuelve 404 sin exigir autenticacion")
    void rutaInexistente() throws Exception {
        mockMvc.perform(post("/v1/no-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("401 authentication-required sin token")
    void rechazaSinToken() throws Exception {
        mockMvc.perform(post("/v1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:authentication-required"));
    }

    @Test
    @DisplayName("403 insufficient-role con un token de rol USER")
    void rechazaRolInsuficiente() throws Exception {
        mockMvc.perform(post("/v1/rooms")
                        .header("Authorization",
                                "Bearer " + TestJwtFactory.tokenFor(USER_ID, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:insufficient-role"));
    }
}
