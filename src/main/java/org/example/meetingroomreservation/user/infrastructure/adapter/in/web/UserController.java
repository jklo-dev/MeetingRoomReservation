package org.example.meetingroomreservation.user.infrastructure.adapter.in.web;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.example.meetingroomreservation.shared.config.OpenApiConfig;
import org.example.meetingroomreservation.user.domain.model.User;
import org.example.meetingroomreservation.user.domain.port.in.AuthenticateUser;
import org.example.meetingroomreservation.user.domain.port.in.AuthenticateUserCommand;
import org.example.meetingroomreservation.user.domain.port.in.CreateUser;
import org.example.meetingroomreservation.user.domain.port.in.CreateUserCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada HTTP para usuarios.
 *
 * <p>La exigencia del rol ADMIN esta declarada en la cadena de seguridad, no aqui, porque no
 * depende de ningun recurso concreto.
 */
@RestController
@RequestMapping("/v1/users")
@Tag(name = "Usuarios", description = "Alta de usuarios y obtencion del token de acceso")
class UserController {

    private final CreateUser createUser;
    private final AuthenticateUser authenticateUser;

    UserController(CreateUser createUser, AuthenticateUser authenticateUser) {
        this.createUser = createUser;
        this.authenticateUser = authenticateUser;
    }

    /**
     * Endpoint publico que emite el token. No exige autenticacion porque es el que la
     * proporciona.
     */
    @Operation(
            summary = "Iniciar sesion",
            description = """
                    Devuelve un token JWT con vigencia de 24 horas, junto a los datos del usuario
                    autenticado. No requiere autenticacion: es el endpoint que la proporciona.

                    Solo se valida que los campos esten presentes. El formato del correo **no** se
                    comprueba: un correo mal formado es una credencial que no existe, asi que la
                    respuesta correcta es 401 y no 400.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales validas"),
            @ApiResponse(responseCode = "400",
                    description = "Falta el correo o la contrasena. `validation-error`",
                    content = @Content),
            @ApiResponse(responseCode = "401",
                    description = """
                            Credenciales invalidas. `invalid-credentials`. La respuesta es
                            **identica** si el correo no existe o si la contrasena es incorrecta,
                            para que no se pueda averiguar que correos estan registrados.
                            """,
                    content = @Content)
    })
    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthenticateUser.Result result = authenticateUser.authenticate(
                new AuthenticateUserCommand(request.email(), request.password()));

        return LoginResponse.of(result.user(), result.token());
    }

    @Operation(
            summary = "Crear un usuario",
            description = """
                    Requiere rol `ADMIN`. El correo se normaliza a minusculas y sin espacios
                    externos antes de comprobar la unicidad, de modo que `Ana@X.com` y
                    `ana@x.com` son el mismo usuario.

                    La contrasena se almacena unicamente como hash BCrypt y no aparece en ninguna
                    respuesta.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado"),
            @ApiResponse(responseCode = "400",
                    description = "Campos invalidos. `validation-error` con el array `errors`",
                    content = @Content),
            @ApiResponse(responseCode = "401",
                    description = "Token ausente, caducado o invalido. `authentication-required`",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "El token no tiene rol ADMIN. `insufficient-role`",
                    content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "Ya existe un usuario con ese correo. `email-already-exists`",
                    content = @Content)
    })
    @PostMapping
    ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User created = createUser.create(new CreateUserCommand(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.role()));

        return ResponseEntity
                .created(URI.create("/v1/users/" + created.id()))
                .body(UserResponse.fromDomain(created));
    }
}
