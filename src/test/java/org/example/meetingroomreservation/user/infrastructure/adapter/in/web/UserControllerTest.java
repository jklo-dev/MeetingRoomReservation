package org.example.meetingroomreservation.user.infrastructure.adapter.in.web;

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

import org.example.meetingroomreservation.shared.config.SecurityConfig;
import org.example.meetingroomreservation.shared.web.GlobalExceptionHandler;
import org.example.meetingroomreservation.shared.web.ProblemResponseWriter;
import org.example.meetingroomreservation.support.TestJwtFactory;
import org.example.meetingroomreservation.user.domain.exception.EmailAlreadyExistsException;
import org.example.meetingroomreservation.user.domain.model.Role;
import org.example.meetingroomreservation.user.domain.model.User;
import org.example.meetingroomreservation.user.domain.port.in.CreateUser;
import org.example.meetingroomreservation.user.domain.port.in.CreateUserCommand;
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
 * Contrato HTTP del endpoint de creacion de usuarios.
 *
 * <p>Sin base de datos: el caso de uso se sustituye por un doble, de modo que estos tests se
 * ejecutan sin Docker.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, ProblemResponseWriter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "meetroom.security.jwt.secret=" + TestJwtFactory.SECRET)
class UserControllerTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static final String BODY_VALIDO = """
            {
              "email": "ana.lopez@empresa.com",
              "password": "clave",
              "firstName": "Ana",
              "lastName": "Lopez",
              "role": "USER"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateUser createUser;

    /** El controlador tambien depende del login; aqui no se ejercita. */
    @MockitoBean
    private org.example.meetingroomreservation.user.domain.port.in.AuthenticateUser authenticateUser;

    private String adminToken() {
        return TestJwtFactory.tokenFor(ADMIN_ID, "ADMIN");
    }

    @Test
    @DisplayName("201 con el cuerpo documentado, cabecera Location y sin contrasena")
    void creaUsuario() throws Exception {
        User creado = User.restore(
                UUID.fromString("3f2b9c7a-5d41-4e8b-9a0c-7e1d2f6b8a34"),
                "ana.lopez@empresa.com", "$2a$10$hashficticio", "Ana", "Lopez", Role.USER,
                OffsetDateTime.parse("2026-11-17T10:00:00+01:00"));
        given(createUser.create(any(CreateUserCommand.class))).willReturn(creado);

        mockMvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/v1/users/3f2b9c7a-5d41-4e8b-9a0c-7e1d2f6b8a34"))
                .andExpect(jsonPath("$.id").value("3f2b9c7a-5d41-4e8b-9a0c-7e1d2f6b8a34"))
                .andExpect(jsonPath("$.email").value("ana.lopez@empresa.com"))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.lastName").value("Lopez"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("400 validation-error con el array errors ordenado por campo")
    void rechazaBodyInvalido() throws Exception {
        String bodyInvalido = """
                {
                  "email": "no-es-un-correo",
                  "password": "abc",
                  "firstName": "",
                  "lastName": "Lopez",
                  "role": "USER"
                }
                """;

        mockMvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:validation-error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/v1/users"))
                // Orden alfabetico por campo: email, firstName, password
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[1].field").value("firstName"))
                .andExpect(jsonPath("$.errors[2].field").value("password"));
    }

    @Test
    @DisplayName("400 validation-error cuando el rol no esta en el enumerado, nombrando el campo")
    void rechazaRolInvalido() throws Exception {
        String bodyRolInvalido = BODY_VALIDO.replace("\"USER\"", "\"SUPERADMIN\"");

        mockMvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyRolInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("role"));
    }

    @Test
    @DisplayName("400 cuando el rol llega en minusculas: el enumerado es sensible a mayusculas")
    void rechazaRolEnMinusculas() throws Exception {
        String bodyRolMinusculas = BODY_VALIDO.replace("\"USER\"", "\"admin\"");

        mockMvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyRolMinusculas))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("409 email-already-exists cuando el correo ya existe")
    void rechazaCorreoDuplicado() throws Exception {
        willThrow(new EmailAlreadyExistsException())
                .given(createUser).create(any(CreateUserCommand.class));

        mockMvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:email-already-exists"))
                .andExpect(jsonPath("$.detail")
                        .value("Ya existe un usuario con el correo indicado"));
    }

    @Test
    @DisplayName("401 authentication-required sin cabecera Authorization")
    void rechazaSinToken() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:authentication-required"))
                .andExpect(jsonPath("$.instance").value("/v1/users"));
    }

    @Test
    @DisplayName("401 con un token caducado")
    void rechazaTokenCaducado() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .header("Authorization",
                                "Bearer " + TestJwtFactory.expiredTokenFor(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:authentication-required"));
    }

    @Test
    @DisplayName("403 insufficient-role con un token de rol USER")
    void rechazaRolInsuficiente() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .header("Authorization",
                                "Bearer " + TestJwtFactory.tokenFor(USER_ID, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:insufficient-role"));
    }

    @Test
    @DisplayName("ninguna respuesta de error usa about:blank")
    void ningunErrorUsaAboutBlank() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(jsonPath("$.type").value(
                        org.hamcrest.Matchers.not("about:blank")));
    }
}
