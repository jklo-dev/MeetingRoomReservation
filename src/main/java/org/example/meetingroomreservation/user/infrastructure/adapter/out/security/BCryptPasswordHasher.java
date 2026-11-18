package org.example.meetingroomreservation.user.infrastructure.adapter.out.security;

import org.example.meetingroomreservation.user.domain.port.out.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptador que satisface {@link PasswordHasher} con BCrypt.
 */
@Component
class BCryptPasswordHasher implements PasswordHasher {

    /**
     * Hash senuelo de coste 10, generado con este mismo codificador a partir de una cadena
     * arbitraria. Su unica funcion es consumir el mismo tiempo que una verificacion real cuando
     * el correo no existe. Ninguna contrasena de ningun usuario corresponde a este hash.
     */
    private static final String DECOY_HASH =
            "$2a$10$wnLG8OnBbq.bqYprNPQat.XZZxIizweD0cbW8YEDZuCcb0Mp01dp6";

    private final PasswordEncoder passwordEncoder;

    BCryptPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }

    @Override
    public boolean matchesDecoy(String rawPassword) {
        // El resultado se descarta: lo que importa es haber pagado el coste de BCrypt.
        return passwordEncoder.matches(rawPassword, DECOY_HASH);
    }
}
