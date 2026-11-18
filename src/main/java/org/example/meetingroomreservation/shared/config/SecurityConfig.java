package org.example.meetingroomreservation.shared.config;

import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.example.meetingroomreservation.shared.web.ProblemResponseWriter;
import org.example.meetingroomreservation.shared.web.ProblemTypes;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Cadena de seguridad de la API.
 *
 * <p>Esta funcionalidad incluye la validacion del token pero no su emision: el endpoint de
 * login llega con la funcionalidad 002.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
// Publica para que los tests de capa web puedan importarla y ejercitar la cadena real,
// en lugar de una configuracion de seguridad distinta a la de produccion.
public class SecurityConfig {

    /** Claim donde viaja el rol, con valor USER o ADMIN. */
    private static final String ROLE_CLAIM = "role";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtDecoder jwtDecoder,
                                    ProblemResponseWriter problemWriter) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, exception) ->
                problemWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                        ProblemTypes.AUTHENTICATION_REQUIRED, "Se requiere autenticacion");

        AccessDeniedHandler forbidden = (request, response, exception) ->
                problemWriter.write(request, response, HttpStatus.FORBIDDEN,
                        ProblemTypes.INSUFFICIENT_ROLE,
                        "No tiene permisos suficientes para esta operacion");

        return http
                // API con token: no hay sesion que proteger de CSRF.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        // El login es el endpoint que proporciona la autenticacion, asi que no
                        // puede exigirla. Debe declararse antes que la regla de /v1/users.
                        .requestMatchers(HttpMethod.POST, "/v1/users/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/v1/rooms").hasRole("ADMIN")
                        // Por el requisito 5 de la prueba, los endpoints de consulta son
                        // publicos.
                        .requestMatchers(HttpMethod.GET, "/v1/rooms").permitAll()
                        // Requiere autenticacion pero ningun rol concreto: cualquier usuario
                        // autenticado puede reservar.
                        .requestMatchers(HttpMethod.POST, "/v1/reservations").authenticated()
                        // Solo exige autenticacion: el permiso depende de comparar el usuario
                        // del token con el responsable de esa reserva concreta, dato que solo se
                        // conoce tras consultarla. La autorizacion se resuelve en el caso de uso.
                        .requestMatchers(HttpMethod.POST, "/v1/reservations/*/cancel")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/v1/reservations").permitAll()
                        // Cualquier otra ruta queda denegada hasta que una funcionalidad
                        // posterior la abra explicitamente.
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        // Imprescindible: el servidor de recursos trae su propio entry point,
                        // que responde 401 sin cuerpo y ganaria sobre el global cuando el fallo
                        // viene de un bearer token invalido o caducado.
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden)
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                // Cubre los casos en los que no se intento autenticar en absoluto.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden))
                .build();
    }

    /**
     * Valida la firma HS256 con la clave simetrica. La misma clave servira para emitir en la
     * funcionalidad 002: si no coincidieran, la API rechazaria sus propios tokens.
     */
    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        return NimbusJwtDecoder.withSecretKey(secretKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Emite los tokens con la **misma** clave que valida el decoder de arriba. Si divergieran,
     * la API rechazaria los tokens que ella misma acaba de emitir.
     */
    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
    }

    private SecretKeySpec secretKey(JwtProperties properties) {
        return new SecretKeySpec(properties.secretBytes(), "HmacSHA256");
    }

    /**
     * Traduce el claim {@code role} a una autoridad de Spring Security.
     *
     * <p>El prefijo {@code ROLE_} lo anade {@link SimpleAuthorityMapper} de forma implicita a
     * traves del convertidor, y es lo que permite usar {@code hasRole("ADMIN")} arriba.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
