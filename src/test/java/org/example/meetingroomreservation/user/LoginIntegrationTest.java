package org.example.meetingroomreservation.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.meetingroomreservation.TestcontainersConfiguration;
import org.example.meetingroomreservation.support.TestJwtFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Circuito completo de autenticacion contra PostgreSQL real.
 *
 * <p><b>Requiere Docker en ejecucion</b>: Testcontainers levanta la base de datos.
 *
 * <p>Es el test que demuestra que las funcionalidades 001 y 002 encajan: inicia sesion con el
 * usuario semilla, usa ese token para crear un usuario nuevo, y despues inicia sesion con las
 * credenciales de ese usuario recien creado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "meetroom.security.jwt.secret=" + TestJwtFactory.SECRET)
class LoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String email, String password) throws Exception {
        String body = """
                { "email": "%s", "password": "%s" }
                """.formatted(email, password);

        String respuesta = mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(respuesta);
        return json.get("accessToken").asString();
    }

    @Test
    @DisplayName("el usuario semilla puede autenticarse con las credenciales documentadas")
    void loginDelUsuarioSemilla() throws Exception {
        mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "admin@meetroom.local", "password": "admin" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andExpect(jsonPath("$.user.email").value("admin@meetroom.local"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    @DisplayName("el correo se normaliza: mayusculas y espacios autentican igual")
    void loginNormalizaElCorreo() throws Exception {
        mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "  ADMIN@Meetroom.LOCAL  ", "password": "admin" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("admin@meetroom.local"));
    }

    @Test
    @DisplayName("circuito completo: login, crear usuario con ese token, y login del nuevo usuario")
    void circuitoCompleto() throws Exception {
        String tokenAdmin = login("admin@meetroom.local", "admin");

        mockMvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "nuevo@empresa.com",
                                  "password": "clave",
                                  "firstName": "Nuevo",
                                  "lastName": "Usuario",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isCreated());

        // El usuario recien creado puede autenticarse: confirma que el hash se guardo bien y que
        // la verificacion lo acepta.
        String tokenNuevo = login("nuevo@empresa.com", "clave");
        assertThat(tokenNuevo).isNotBlank().isNotEqualTo(tokenAdmin);

        // Y su token no sirve para crear usuarios, porque su rol es USER.
        mockMvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + tokenNuevo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "otro@empresa.com",
                                  "password": "clave",
                                  "firstName": "Otro",
                                  "lastName": "Usuario",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:insufficient-role"));
    }

    @Test
    @DisplayName("401 con contrasena incorrecta, y cuerpo identico al de un correo inexistente")
    void credencialesInvalidasSonIndistinguibles() throws Exception {
        String porContrasena = mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "admin@meetroom.local", "password": "incorrecta" }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:invalid-credentials"))
                .andReturn().getResponse().getContentAsString();

        String porCorreo = mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "nadie@empresa.com", "password": "incorrecta" }
                                """))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(porContrasena).isEqualTo(porCorreo);
    }
}
