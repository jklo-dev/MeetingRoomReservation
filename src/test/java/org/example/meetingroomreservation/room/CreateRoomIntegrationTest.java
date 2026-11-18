package org.example.meetingroomreservation.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.example.meetingroomreservation.TestcontainersConfiguration;
import org.example.meetingroomreservation.support.TestJwtFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integracion de creacion de salas contra PostgreSQL real.
 *
 * <p><b>Requiere Docker en ejecucion</b>: Testcontainers levanta la base de datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "meetroom.security.jwt.secret=" + TestJwtFactory.SECRET)
class CreateRoomIntegrationTest {

    private static final UUID ADMIN_SEMILLA =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken() {
        return TestJwtFactory.tokenFor(ADMIN_SEMILLA, "ADMIN");
    }

    private String body(String name, int capacity) {
        return """
                { "name": "%s", "capacity": %d }
                """.formatted(name, capacity);
    }

    @Test
    @DisplayName("201 y la sala queda persistida con su capitalizacion original")
    void creaSalaRealmente() throws Exception {
        mockMvc.perform(post("/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("  Sala Verde  ", 8)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sala Verde"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        Map<String, Object> fila = jdbcTemplate.queryForMap(
                "SELECT name, capacity, status FROM rooms WHERE lower(name) = 'sala verde'");

        assertThat(fila.get("name")).isEqualTo("Sala Verde");
        assertThat(fila.get("capacity")).isEqualTo(8);
        assertThat(fila.get("status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("409 al repetir el nombre con otra capitalizacion")
    void rechazaNombreDuplicado() throws Exception {
        mockMvc.perform(post("/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Sala Azul", 12)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("SALA AZUL", 20)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:room-name-already-exists"));
    }

    @Test
    @DisplayName("acepta los extremos de capacidad, 1 y 1000")
    void aceptaExtremosDeCapacidad() throws Exception {
        mockMvc.perform(post("/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Cabina Individual", 1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/rooms")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Auditorio", 1000)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("el indice unico rechaza duplicados insertados por fuera de la aplicacion")
    void elIndiceProtegeFrenteAEscriturasExternas() {
        // Criterio de aceptacion 8: la invariante vive en los datos, no solo en el codigo. Este
        // es el unico test que lo demuestra, porque no pasa por el caso de uso.
        jdbcTemplate.update(
                "INSERT INTO rooms (id, name, capacity, status, created_at) "
                        + "VALUES (?, ?, ?, ?, now())",
                UUID.randomUUID(), "Sala Externa", 10, "ACTIVE");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO rooms (id, name, capacity, status, created_at) "
                        + "VALUES (?, ?, ?, ?, now())",
                UUID.randomUUID(), "SALA EXTERNA", 20, "ACTIVE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("la restriccion de capacidad rechaza valores fuera de rango en base de datos")
    void laRestriccionDeCapacidadProtegeLosDatos() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO rooms (id, name, capacity, status, created_at) "
                        + "VALUES (?, ?, ?, ?, now())",
                UUID.randomUUID(), "Sala Imposible", 5000, "ACTIVE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("un token de rol USER no puede crear salas")
    void usuarioNormalNoPuedeCrearSalas() throws Exception {
        mockMvc.perform(post("/v1/rooms")
                        .header("Authorization", "Bearer " + TestJwtFactory.tokenFor(
                                UUID.randomUUID(), "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Sala Prohibida", 6)))
                .andExpect(status().isForbidden());
    }
}
