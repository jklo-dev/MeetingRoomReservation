package org.example.meetingroomreservation.user.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.example.meetingroomreservation.user.domain.exception.EmailAlreadyExistsException;
import org.example.meetingroomreservation.user.domain.model.Role;
import org.example.meetingroomreservation.user.domain.model.User;
import org.example.meetingroomreservation.user.domain.port.in.CreateUserCommand;
import org.example.meetingroomreservation.user.domain.port.out.PasswordHasher;
import org.example.meetingroomreservation.user.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Caso de uso con dobles escritos a mano. Sin Spring y sin base de datos.
 */
class CreateUserServiceTest {

    private static final Clock RELOJ_FIJO =
            Clock.fixed(Instant.parse("2026-11-17T09:00:00Z"), ZoneOffset.UTC);

    /** Repositorio en memoria que registra lo que se le pide. */
    private static final class FakeUserRepository implements UserRepositoryPort {
        private final List<String> correosExistentes = new ArrayList<>();
        private final List<User> guardados = new ArrayList<>();
        private String ultimoCorreoConsultado;

        @Override
        public boolean existsByEmail(String normalizedEmail) {
            this.ultimoCorreoConsultado = normalizedEmail;
            return correosExistentes.contains(normalizedEmail);
        }

        @Override
        public User save(User user) {
            guardados.add(user);
            return user;
        }
    }

    /** Hasheador predecible, para distinguir el hash de la contrasena en claro. */
    private static final class FakePasswordHasher implements PasswordHasher {
        private String ultimaContrasenaRecibida;

        @Override
        public String hash(String rawPassword) {
            this.ultimaContrasenaRecibida = rawPassword;
            return "hash(" + rawPassword + ")";
        }
    }

    private final FakeUserRepository repository = new FakeUserRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final CreateUserService service =
            new CreateUserService(repository, hasher, RELOJ_FIJO);

    @Test
    @DisplayName("crea el usuario con el correo normalizado y el instante del reloj inyectado")
    void creaElUsuario() {
        User creado = service.create(new CreateUserCommand(
                "  Ana.Lopez@Empresa.COM  ", "clave", "Ana", "Lopez", Role.USER));

        assertThat(creado.email()).isEqualTo("ana.lopez@empresa.com");
        assertThat(creado.role()).isEqualTo(Role.USER);
        assertThat(creado.createdAt())
                .isEqualTo(OffsetDateTime.parse("2026-11-17T09:00:00Z"));
        assertThat(repository.guardados).containsExactly(creado);
    }

    @Test
    @DisplayName("la contrasena se delega al hasheador y nunca se guarda en claro")
    void delegaElHasheado() {
        User creado = service.create(new CreateUserCommand(
                "a@b.com", "clave-secreta", "Ana", "Lopez", Role.USER));

        assertThat(hasher.ultimaContrasenaRecibida).isEqualTo("clave-secreta");
        assertThat(creado.passwordHash()).isEqualTo("hash(clave-secreta)");
        assertThat(creado.passwordHash()).isNotEqualTo("clave-secreta");
    }

    @Test
    @DisplayName("la unicidad se comprueba sobre el correo ya normalizado")
    void compruebaUnicidadNormalizada() {
        service.create(new CreateUserCommand(
                "  Ana.Lopez@EMPRESA.com ", "clave", "Ana", "Lopez", Role.USER));

        assertThat(repository.ultimoCorreoConsultado).isEqualTo("ana.lopez@empresa.com");
    }

    @Test
    @DisplayName("falla con 409 de dominio si el correo ya existe, en cualquier capitalizacion")
    void rechazaCorreoDuplicado() {
        repository.correosExistentes.add("ana.lopez@empresa.com");

        assertThatThrownBy(() -> service.create(new CreateUserCommand(
                "ANA.LOPEZ@Empresa.com", "clave", "Ana", "Lopez", Role.USER)))
                .isInstanceOf(EmailAlreadyExistsException.class);

        assertThat(repository.guardados).isEmpty();
    }

    @Test
    @DisplayName("no hashea la contrasena si el correo ya existe")
    void noHasheaSiYaExiste() {
        repository.correosExistentes.add("a@b.com");

        assertThatThrownBy(() -> service.create(new CreateUserCommand(
                "a@b.com", "clave", "Ana", "Lopez", Role.USER)))
                .isInstanceOf(EmailAlreadyExistsException.class);

        assertThat(hasher.ultimaContrasenaRecibida).isNull();
    }

    @Test
    @DisplayName("el comando no filtra la contrasena en toString")
    void comandoNoFiltraContrasena() {
        CreateUserCommand command = new CreateUserCommand(
                "a@b.com", "clave-secreta", "Ana", "Lopez", Role.USER);

        assertThat(command.toString()).doesNotContain("clave-secreta");
    }
}
