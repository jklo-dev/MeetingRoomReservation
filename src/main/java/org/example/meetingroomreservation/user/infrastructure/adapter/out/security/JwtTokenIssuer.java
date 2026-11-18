package org.example.meetingroomreservation.user.infrastructure.adapter.out.security;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.example.meetingroomreservation.shared.config.JwtProperties;
import org.example.meetingroomreservation.user.domain.model.IssuedToken;
import org.example.meetingroomreservation.user.domain.model.User;
import org.example.meetingroomreservation.user.domain.port.out.TokenIssuer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Emite el token JWT firmado con HS256.
 *
 * <p>Usa la misma clave simetrica que el {@code JwtDecoder} de la cadena de seguridad: si no
 * coincidieran, la API rechazaria sus propios tokens.
 */
@Component
class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    JwtTokenIssuer(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(User user) {
        // Se truncan los segundos porque iat y exp son numericos en segundos: sin truncar, la
        // diferencia entre ambos podria no ser exactamente el ttl configurado.
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plusSeconds(properties.getTtlSeconds());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.id().toString())
                .claim("role", user.role().name())
                .issuer(properties.getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new IssuedToken(value, properties.getTtlSeconds());
    }
}
