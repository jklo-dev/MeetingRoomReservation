package org.example.meetingroomreservation.shared.config;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de firma y validacion del token.
 *
 * <p>El secreto se lee de configuracion externa, nunca del repositorio.
 */
@ConfigurationProperties(prefix = "meetroom.security.jwt")
public class JwtProperties {

    /** HS256 exige una clave de al menos 256 bits. */
    private static final int MIN_SECRET_BYTES = 32;

    private String secret;
    private String issuer = "meetroom";
    private long ttlSeconds = 86400;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * Falla al arrancar, con un mensaje accionable, en lugar de dejar que Nimbus lance un error
     * oscuro en la primera peticion.
     */
    public byte[] secretBytes() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Falta la variable de entorno JWT_SECRET: define una cadena de al menos "
                            + MIN_SECRET_BYTES + " caracteres");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET es demasiado corto: HS256 exige al menos " + MIN_SECRET_BYTES
                            + " bytes y se han recibido " + bytes.length);
        }
        return bytes;
    }
}
