package org.example.meetingroomreservation.user.infrastructure.adapter.out.security;

import org.example.meetingroomreservation.user.domain.port.out.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptador que satisface {@link PasswordHasher} con BCrypt.
 */
@Component
class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    BCryptPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
