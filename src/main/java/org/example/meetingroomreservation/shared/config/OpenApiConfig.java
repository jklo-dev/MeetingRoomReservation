package org.example.meetingroomreservation.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos del documento OpenAPI y esquema de seguridad.
 *
 * <p>El esquema {@code bearerAuth} es lo que habilita el boton «Authorize» de Swagger UI: sin el,
 * la interfaz no permite enviar el token y los endpoints protegidos solo se podrian probar desde
 * fuera.
 */
@Configuration
// Publica porque los controladores referencian BEARER_SCHEME desde sus propios paquetes.
public class OpenApiConfig {

    /** Nombre del esquema de seguridad, referenciado por las anotaciones de los controladores. */
    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MeetingRoomReservation API")
                        .version("0.0.1-SNAPSHOT")
                        .description("""
                                API REST para reservar salas de reuniones.

                                **Autenticacion**: obten un token en `POST /v1/users/login` y
                                pulsa «Authorize» pegando solo el valor de `accessToken`, sin el
                                prefijo `Bearer`. El usuario semilla es
                                `admin@meetroom.local` / `admin`.

                                **Errores**: todas las respuestas de error siguen RFC 7807 con
                                tipo de contenido `application/problem+json`. Cada causa distinta
                                tiene su propio `type` con el esquema
                                `urn:meetroom:problem:<slug>`, de modo que un cliente puede
                                ramificar por `type` sin interpretar el `detail`. El array
                                `errors` aparece solo en los errores de validacion de campos.

                                **Fechas**: todos los instantes son ISO-8601 **con offset
                                obligatorio**, por ejemplo `2026-12-01T10:00:00-05:00`. Una fecha
                                sin zona se rechaza.
                                """))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token devuelto por POST /v1/users/login")));
    }
}
