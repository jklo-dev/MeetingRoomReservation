package org.example.meetingroomreservation.user.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Invariantes del modelo de dominio. Sin Spring: se ejecutan sin contexto ni base de datos.
 */
class UserTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-17T10:00:00+01:00");

    @Test
    @DisplayName("el correo se recorta y se pasa a minusculas")
    void normalizaElCorreo() {
        User user = User.create("  Ana.Lopez@Empresa.COM  ", "hash", "Ana", "Lopez", Role.USER,
                AHORA);

        assertThat(user.email()).isEqualTo("ana.lopez@empresa.com");
    }

    @Test
    @DisplayName("normalizeEmail aplica la misma regla que la construccion")
    void normalizeEmailEsConsistente() {
        // La comprobacion de unicidad usa este metodo, asi que debe coincidir exactamente con
        // lo que se almacena.
        assertThat(User.normalizeEmail(" ADMIN@Meetroom.Local "))
                .isEqualTo("admin@meetroom.local");
    }

    @Test
    @DisplayName("el nombre y el apellido se recortan")
    void recortaNombreYApellido() {
        User user = User.create("a@b.com", "hash", "  Ana  ", "  Lopez  ", Role.USER, AHORA);

        assertThat(user.firstName()).isEqualTo("Ana");
        assertThat(user.lastName()).isEqualTo("Lopez");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("rechaza un nombre vacio o solo con espacios")
    void rechazaNombreVacio(String nombre) {
        assertThatThrownBy(() -> User.create("a@b.com", "hash", nombre, "Lopez", Role.USER, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("rechaza un apellido vacio o solo con espacios")
    void rechazaApellidoVacio(String apellido) {
        assertThatThrownBy(() -> User.create("a@b.com", "hash", "Ana", apellido, Role.USER, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rechaza un correo nulo o vacio")
    void rechazaCorreoVacio() {
        assertThatThrownBy(() -> User.create("   ", "hash", "Ana", "Lopez", Role.USER, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rechaza un correo que excede 255 caracteres tras normalizar")
    void rechazaCorreoDemasiadoLargo() {
        String largo = "a".repeat(250) + "@b.com";

        assertThatThrownBy(() -> User.create(largo, "hash", "Ana", "Lopez", Role.USER, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rechaza un hash de contrasena ausente")
    void rechazaHashAusente() {
        assertThatThrownBy(() -> User.create("a@b.com", "  ", "Ana", "Lopez", Role.USER, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("genera el identificador en el dominio")
    void generaIdentificador() {
        User uno = User.create("a@b.com", "hash", "Ana", "Lopez", Role.USER, AHORA);
        User otro = User.create("c@d.com", "hash", "Ana", "Lopez", Role.USER, AHORA);

        assertThat(uno.id()).isNotNull().isNotEqualTo(otro.id());
    }

    @Test
    @DisplayName("toString no filtra el hash de la contrasena")
    void toStringNoFiltraElHash() {
        User user = User.create("a@b.com", "hash-secreto", "Ana", "Lopez", Role.USER, AHORA);

        assertThat(user.toString()).doesNotContain("hash-secreto");
    }
}
