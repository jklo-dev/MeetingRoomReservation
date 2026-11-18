package org.example.meetingroomreservation.user.infrastructure.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.example.meetingroomreservation.shared.config.JwtProperties;
import org.example.meetingroomreservation.user.domain.model.IssuedToken;
import org.example.meetingroomreservation.user.domain.model.Role;
import org.example.meetingroomreservation.user.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.time.OffsetDateTime;

/**
 * Emision del token con reloj fijo, para comprobar los claims y la caducidad sin esperas.
 *
 * <p>Verifica tambien lo mas importante de la 002: que el token emitido sea aceptado por el
 * mismo decoder que usa la cadena de seguridad. Si las claves divergieran, la API rechazaria
 * sus propios tokens.
 */
class JwtTokenIssuerTest {

    private static final String SECRET = "secreto-de-pruebas-con-mas-de-32-bytes";
    private static final Instant AHORA = Instant.parse("2026-11-17T09:00:00Z");

    private static final User ANA = User.restore(
            UUID.fromString("3f2b9c7a-5d41-4e8b-9a0c-7e1d2f6b8a34"),
            "ana.lopez@empresa.com", "$2a$10$hashficticio", "Ana", "Lopez", Role.USER,
            OffsetDateTime.parse("2026-11-17T10:00:00+01:00"));

    private final JwtProperties properties = properties();
    private final SecretKeySpec key = new SecretKeySpec(properties.secretBytes(), "HmacSHA256");

    private final JwtTokenIssuer issuer = new JwtTokenIssuer(
            new NimbusJwtEncoder(new ImmutableSecret<>(key)),
            properties,
            Clock.fixed(AHORA, ZoneOffset.UTC));

    private final JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

    private static JwtProperties properties() {
        JwtProperties p = new JwtProperties();
        p.setSecret(SECRET);
        p.setIssuer("meetroom");
        p.setTtlSeconds(86400);
        return p;
    }

    @Test
    @DisplayName("emite los claims sub, role, iat, exp e iss")
    void emiteLosClaims() {
        IssuedToken token = issuer.issue(ANA);
        Jwt jwt = decoder.decode(token.value());

        // El identificador va en sub y no el correo: el correo podria cambiar, el UUID no.
        assertThat(jwt.getSubject()).isEqualTo(ANA.id().toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        // Se lee el claim en crudo y no con jwt.getIssuer(): ese accessor devuelve una URL y
        // lanza IllegalArgumentException con "meetroom", que no lo es. El token es valido -RFC
        // 7519 admite iss como cadena simple-, pero conviene saber que cualquier consumidor que
        // use el accessor tipado fallaria. Cambiarlo exigiria un emisor con forma de URL.
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("meetroom");
        assertThat(jwt.getIssuedAt()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("la caducidad son exactamente 24 horas despues de la emision")
    void caducaEnVeinticuatroHoras() {
        IssuedToken token = issuer.issue(ANA);
        Jwt jwt = decoder.decode(token.value());

        assertThat(token.expiresInSeconds()).isEqualTo(86400);
        assertThat(jwt.getExpiresAt()).isEqualTo(AHORA.plusSeconds(86400));
        assertThat(java.time.Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toHours())
                .isEqualTo(24);
    }

    @Test
    @DisplayName("el rol emitido es el del usuario, no un valor fijo")
    void emiteElRolDelUsuario() {
        User admin = User.restore(UUID.randomUUID(), "admin@meetroom.local", "$2a$10$x",
                "Admin", "Sistema", Role.ADMIN, OffsetDateTime.parse("2026-11-17T10:00:00+01:00"));

        Jwt jwt = decoder.decode(issuer.issue(admin).value());

        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("el token emitido lo acepta el decoder de la cadena de seguridad")
    void elTokenEsValidoParaLaPropiaApi() {
        IssuedToken token = issuer.issue(ANA);

        // Si esto fallara, la API rechazaria los tokens que ella misma emite.
        assertThat(decoder.decode(token.value())).isNotNull();
    }

    @Test
    @DisplayName("no emite aud ni jti")
    void noEmiteClaimsInnecesarios() {
        Jwt jwt = decoder.decode(issuer.issue(ANA).value());

        assertThat(jwt.getAudience()).isNullOrEmpty();
        assertThat(jwt.getId()).isNull();
    }
}
