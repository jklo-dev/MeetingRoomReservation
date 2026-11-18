package org.example.meetingroomreservation.user.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.example.meetingroomreservation.shared.config.SecurityConfig;
import org.example.meetingroomreservation.shared.web.GlobalExceptionHandler;
import org.example.meetingroomreservation.shared.web.ProblemResponseWriter;
import org.example.meetingroomreservation.support.TestJwtFactory;
import org.example.meetingroomreservation.user.domain.exception.InvalidCredentialsException;
import org.example.meetingroomreservation.user.domain.model.IssuedToken;
import org.example.meetingroomreservation.user.domain.model.Role;
import org.example.meetingroomreservation.user.domain.model.User;
import org.example.meetingroomreservation.user.domain.port.in.AuthenticateUser;
import org.example.meetingroomreservation.user.domain.port.in.AuthenticateUserCommand;
import org.example.meetingroomreservation.user.domain.port.in.CreateUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato HTTP del endpoint de login. Sin base de datos: el caso de uso es un doble.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, ProblemResponseWriter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "meetroom.security.jwt.secret=" + TestJwtFactory.SECRET)
class UserLoginControllerTest {

    private static final User ADMIN = User.restore(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "admin@meetroom.local", "$2a$10$hashficticio", "Admin", "Sistema", Role.ADMIN,
            OffsetDateTime.parse("2026-11-17T10:00:00+01:00"));

    private static final String CREDENCIALES = """
            {
              "email": "admin@meetroom.local",
              "password": "admin"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateUser authenticateUser;

    /** El controlador tambien depende de la creacion; aqui no se ejercita. */
    @MockitoBean
    private CreateUser createUser;

    @Test
    @DisplayName("200 con token, tipo, caducidad y datos del usuario")
    void autenticaCorrectamente() throws Exception {
        given(authenticateUser.authenticate(any(AuthenticateUserCommand.class)))
                .willReturn(new AuthenticateUser.Result(
                        ADMIN, new IssuedToken("el-token-firmado", 86400)));

        mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREDENCIALES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("el-token-firmado"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andExpect(jsonPath("$.user.id")
                        .value("00000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.user.email").value("admin@meetroom.local"))
                .andExpect(jsonPath("$.user.firstName").value("Admin"))
                .andExpect(jsonPath("$.user.lastName").value("Sistema"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                // Nunca se devuelve la contrasena ni su hash.
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("el login es publico: funciona sin cabecera Authorization")
    void esPublico() throws Exception {
        given(authenticateUser.authenticate(any(AuthenticateUserCommand.class)))
                .willReturn(new AuthenticateUser.Result(
                        ADMIN, new IssuedToken("el-token-firmado", 86400)));

        mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREDENCIALES))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("401 invalid-credentials cuando las credenciales no son validas")
    void rechazaCredencialesInvalidas() throws Exception {
        willThrow(new InvalidCredentialsException())
                .given(authenticateUser).authenticate(any(AuthenticateUserCommand.class));

        mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREDENCIALES))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:invalid-credentials"))
                .andExpect(jsonPath("$.detail").value("Credenciales invalidas"))
                .andExpect(jsonPath("$.instance").value("/v1/users/login"));
    }

    @Test
    @DisplayName("los 401 por correo inexistente y por contrasena incorrecta son identicos")
    void losDosFallosSonIndistinguibles() throws Exception {
        // El caso de uso lanza la misma excepcion en ambos casos, asi que el cuerpo HTTP debe
        // ser identico: cualquier diferencia permitiria averiguar que correos existen.
        willThrow(new InvalidCredentialsException())
                .given(authenticateUser).authenticate(any(AuthenticateUserCommand.class));

        String porCorreoInexistente = mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "nadie@empresa.com", "password": "x" }
                                """))
                .andReturn().getResponse().getContentAsString();

        String porContrasenaIncorrecta = mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "admin@meetroom.local", "password": "incorrecta" }
                                """))
                .andReturn().getResponse().getContentAsString();

        assertThat(porCorreoInexistente).isEqualTo(porContrasenaIncorrecta);
    }

    @Test
    @DisplayName("400 validation-error cuando falta la contrasena")
    void rechazaContrasenaAusente() throws Exception {
        mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "admin@meetroom.local" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    @DisplayName("400 cuando el correo esta vacio")
    void rechazaCorreoVacio() throws Exception {
        mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "   ", "password": "admin" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    @DisplayName("un correo mal formado no es 400 sino 401: es una credencial que no existe")
    void correoMalFormadoDevuelve401() throws Exception {
        // A diferencia de la creacion, el login no valida el formato del correo. Asi las reglas
        // de formato pueden endurecerse sin dejar fuera a usuarios ya creados.
        willThrow(new InvalidCredentialsException())
                .given(authenticateUser).authenticate(any(AuthenticateUserCommand.class));

        mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "no-es-un-correo", "password": "admin" }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:invalid-credentials"));
    }

    @Test
    @DisplayName("400 malformed-request con JSON mal formado")
    void rechazaJsonMalFormado() throws Exception {
        mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:malformed-request"));
    }
}
