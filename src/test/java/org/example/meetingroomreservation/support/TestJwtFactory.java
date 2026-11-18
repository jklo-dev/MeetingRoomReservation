package org.example.meetingroomreservation.support;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Firma tokens HS256 con la misma clave que valida la aplicacion.
 *
 * <p>La funcionalidad 001 incluye la validacion del token pero no su emision: el login llega
 * con la 002. Hasta entonces este auxiliar es la unica forma de obtener un token, tal como
 * recoge la especificacion.
 */
public final class TestJwtFactory {

    /** Debe tener al menos 32 bytes: HS256 lo exige. */
    public static final String SECRET = "secreto-de-pruebas-con-mas-de-32-bytes";

    private TestJwtFactory() {
    }

    public static String tokenFor(UUID userId, String role) {
        return token(userId.toString(), role, Instant.now().plusSeconds(3600));
    }

    public static String tokenForSubject(String subject, String role) {
        return token(subject, role, Instant.now().plusSeconds(3600));
    }

    public static String expiredTokenFor(UUID userId, String role) {
        return token(userId.toString(), role, Instant.now().minusSeconds(60));
    }

    private static String token(String subject, String role, Instant expiration) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .claim("role", role)
                    .issuer("meetroom")
                    .issueTime(Date.from(Instant.now().minusSeconds(5)))
                    .expirationTime(Date.from(expiration))
                    .build();

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("no se pudo firmar el token de pruebas", e);
        }
    }
}
