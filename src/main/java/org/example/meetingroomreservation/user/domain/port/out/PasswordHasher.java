package org.example.meetingroomreservation.user.domain.port.out;

/**
 * Puerto de salida de hasheado de contrasenas.
 *
 * <p>El dominio no conoce el algoritmo: la implementacion decide que es BCrypt.
 * La funcionalidad 002 ampliara este puerto con la verificacion.
 */
public interface PasswordHasher {

    String hash(String rawPassword);
}
