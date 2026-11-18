package org.example.meetingroomreservation.user.domain.port.out;

/**
 * Puerto de salida de hasheado y verificacion de contrasenas.
 *
 * <p>El dominio no conoce el algoritmo: la implementacion decide que es BCrypt.
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);

    /**
     * Compara la contrasena contra un hash senuelo constante y devuelve siempre {@code false}.
     *
     * <p>Existe para igualar tiempos de respuesta cuando el correo no corresponde a ningun
     * usuario. Sin esta comparacion, autenticarse con un correo inexistente seria
     * perceptiblemente mas rapido que con uno real, porque se saltaria el coste de BCrypt, y esa
     * diferencia de latencia permitiria enumerar que cuentas existen.
     *
     * <p>El senuelo vive en la implementacion: el dominio solo expresa la intencion de gastar el
     * mismo tiempo, sin saber que hay un hash detras.
     */
    boolean matchesDecoy(String rawPassword);
}
