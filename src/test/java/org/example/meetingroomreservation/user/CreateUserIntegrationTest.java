package org.example.meetingroomreservation.user;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integracion de extremo a extremo contra PostgreSQL real.
 *
 * <p><b>Requiere Docker en ejecucion</b>: Testcontainers levanta la base de datos. Verifica que
 * las migraciones de Flyway se aplican desde cero, que el hash se persiste correctamente y que
 * la restriccion unica produce el 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "meetroom.security.jwt.secret=" + TestJwtFactory.SECRET)
class CreateUserIntegrationTest {

    private static final UUID ADMIN_SEMILLA =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken() {
        return TestJwtFactory.tokenFor(ADMIN_SEMILLA, "ADMIN");
    }

    private String bodyCon(String email) {
        return """
                {
                  "email": "%s",
                  "password": "clave",
                  "firstName": "Ana",
                  "lastName": "Lopez",
                  "role": "USER"
                }
                """.formatted(email);
    }

    @Test
    @DisplayName("las migraciones se aplican y el usuario semilla existe con hash BCrypt valido")
    void migracionesYUsuarioSemilla() {
        Map<String, Object> admin = jdbcTemplate.queryForMap(
                "SELECT email, password_hash, role FROM users WHERE id = ?", ADMIN_SEMILLA);

        assertThat(admin.get("email")).isEqualTo("admin@meetroom.local");
        assertThat(admin.get("role")).isEqualTo("ADMIN");
        // El hash se genero con el codificador real, asi que debe verificar la contrasena
        // documentada en el README.
        assertThat(passwordEncoder.matches("admin", (String) admin.get("password_hash"))).isTrue();
    }

    @Test
    @DisplayName("201 y el usuario queda persistido con el correo normalizado y la clave hasheada")
    void creaUsuarioRealmente() throws Exception {
        String respuesta = mockMvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCon("  Nueva.Persona@Empresa.COM  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("nueva.persona@empresa.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(respuesta).doesNotContain("clave");

        Map<String, Object> fila = jdbcTemplate.queryForMap(
                "SELECT email, password_hash FROM users WHERE email = ?",
                "nueva.persona@empresa.com");

        String hash = (String) fila.get("password_hash");
        assertThat(hash).isNotEqualTo("clave").hasSize(60);
        assertThat(passwordEncoder.matches("clave", hash)).isTrue();
    }

    @Test
    @DisplayName("409 al repetir el correo en otra capitalizacion")
    void rechazaCorreoDuplicado() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCon("duplicado@empresa.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCon("DUPLICADO@Empresa.COM")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:email-already-exists"));
    }

    @Test
    @DisplayName("409 tambien cuando el correo del usuario semilla se reutiliza")
    void rechazaCorreoDelSemilla() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCon("ADMIN@MEETROOM.LOCAL")))
                .andExpect(status().isConflict());
    }
}
