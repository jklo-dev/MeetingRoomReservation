package org.example.meetingroomreservation.user.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.example.meetingroomreservation.user.domain.exception.InvalidCredentialsException;
import org.example.meetingroomreservation.user.domain.model.IssuedToken;
import org.example.meetingroomreservation.user.domain.model.Role;
import org.example.meetingroomreservation.user.domain.model.User;
import org.example.meetingroomreservation.user.domain.port.in.AuthenticateUser;
import org.example.meetingroomreservation.user.domain.port.in.AuthenticateUserCommand;
import org.example.meetingroomreservation.user.domain.port.out.PasswordHasher;
import org.example.meetingroomreservation.user.domain.port.out.TokenIssuer;
import org.example.meetingroomreservation.user.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Caso de uso de autenticacion con dobles escritos a mano. Sin Spring ni base de datos.
 */
class AuthenticateUserServiceTest {

    private static final User ANA = User.restore(
            UUID.fromString("3f2b9c7a-5d41-4e8b-9a0c-7e1d2f6b8a34"),
            "ana.lopez@empresa.com",
            "hash(clave-correcta)",
            "Ana", "Lopez", Role.USER,
            OffsetDateTime.parse("2026-11-17T10:00:00+01:00"));

    private static final class FakeUserRepository implements UserRepositoryPort {
        private final List<User> existentes = new ArrayList<>();
        private String ultimoCorreoConsultado;

        @Override
        public boolean existsByEmail(String normalizedEmail) {
            return findByEmail(normalizedEmail).isPresent();
        }

        @Override
        public Optional<User> findByEmail(String normalizedEmail) {
            this.ultimoCorreoConsultado = normalizedEmail;
            return existentes.stream().filter(u -> u.email().equals(normalizedEmail)).findFirst();
        }

        @Override
        public User save(User user) {
            existentes.add(user);
            return user;
        }
    }

    private static final class FakePasswordHasher implements PasswordHasher {
        private int vecesSenuelo;
        private int vecesVerificacionReal;

        @Override
        public String hash(String rawPassword) {
            return "hash(" + rawPassword + ")";
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            vecesVerificacionReal++;
            return passwordHash.equals("hash(" + rawPassword + ")");
        }

        @Override
        public boolean matchesDecoy(String rawPassword) {
            vecesSenuelo++;
            return false;
        }
    }

    private static final class FakeTokenIssuer implements TokenIssuer {
        private User ultimoUsuario;

        @Override
        public IssuedToken issue(User user) {
            this.ultimoUsuario = user;
            return new IssuedToken("token-para-" + user.id(), 86400);
        }
    }

    private final FakeUserRepository repository = new FakeUserRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final FakeTokenIssuer tokenIssuer = new FakeTokenIssuer();
    private final AuthenticateUserService service =
            new AuthenticateUserService(repository, hasher, tokenIssuer);

    @Test
    @DisplayName("credenciales correctas devuelven el usuario y un token")
    void autenticaCorrectamente() {
        repository.save(ANA);

        AuthenticateUser.Result result = service.authenticate(
                new AuthenticateUserCommand("ana.lopez@empresa.com", "clave-correcta"));

        assertThat(result.user()).isEqualTo(ANA);
        assertThat(result.token().value()).isEqualTo("token-para-" + ANA.id());
        assertThat(result.token().expiresInSeconds()).isEqualTo(86400);
        assertThat(tokenIssuer.ultimoUsuario).isEqualTo(ANA);
    }

    @Test
    @DisplayName("el correo se normaliza antes de buscar")
    void normalizaElCorreo() {
        repository.save(ANA);

        service.authenticate(new AuthenticateUserCommand(
                "  ANA.Lopez@Empresa.COM  ", "clave-correcta"));

        assertThat(repository.ultimoCorreoConsultado).isEqualTo("ana.lopez@empresa.com");
    }

    @Test
    @DisplayName("contrasena incorrecta lanza credenciales invalidas y no emite token")
    void rechazaContrasenaIncorrecta() {
        repository.save(ANA);

        assertThatThrownBy(() -> service.authenticate(
                new AuthenticateUserCommand("ana.lopez@empresa.com", "clave-equivocada")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(tokenIssuer.ultimoUsuario).isNull();
    }

    @Test
    @DisplayName("correo inexistente lanza la misma excepcion y ejecuta la comparacion senuelo")
    void rechazaCorreoInexistente() {
        assertThatThrownBy(() -> service.authenticate(
                new AuthenticateUserCommand("nadie@empresa.com", "cualquiera")))
                .isInstanceOf(InvalidCredentialsException.class);

        // La comparacion senuelo es lo que iguala los tiempos de respuesta: sin ella, un correo
        // inexistente responderia mas rapido y permitiria enumerar cuentas.
        assertThat(hasher.vecesSenuelo).isEqualTo(1);
        assertThat(tokenIssuer.ultimoUsuario).isNull();
    }

    @Test
    @DisplayName("un correo con formato invalido devuelve credenciales invalidas, no un error de validacion")
    void correoMalFormadoEsCredencialInvalida() {
        assertThatThrownBy(() -> service.authenticate(
                new AuthenticateUserCommand("no-es-un-correo", "cualquiera")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("un correo vacio devuelve credenciales invalidas, no un fallo del dominio")
    void correoVacioEsCredencialInvalida() {
        assertThatThrownBy(() -> service.authenticate(
                new AuthenticateUserCommand("   ", "cualquiera")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("ambos fallos son indistinguibles: mismo tipo y mismo mensaje")
    void ambosFallosSonIndistinguibles() {
        repository.save(ANA);

        Throwable porContrasena = org.assertj.core.api.Assertions.catchThrowable(() ->
                service.authenticate(new AuthenticateUserCommand(
                        "ana.lopez@empresa.com", "clave-equivocada")));
        Throwable porCorreo = org.assertj.core.api.Assertions.catchThrowable(() ->
                service.authenticate(new AuthenticateUserCommand(
                        "nadie@empresa.com", "cualquiera")));

        assertThat(porContrasena).hasSameClassAs(porCorreo);
        assertThat(porContrasena.getMessage()).isEqualTo(porCorreo.getMessage());
    }

    @Test
    @DisplayName("el comando no filtra la contrasena en toString")
    void comandoNoFiltraContrasena() {
        AuthenticateUserCommand command =
                new AuthenticateUserCommand("a@b.com", "clave-secreta");

        assertThat(command.toString()).doesNotContain("clave-secreta");
    }

    @Test
    @DisplayName("el token no aparece en el toString de IssuedToken")
    void tokenNoApareceEnToString() {
        IssuedToken token = new IssuedToken("valor-secreto-del-token", 86400);

        assertThat(token.toString()).doesNotContain("valor-secreto-del-token");
    }
}
