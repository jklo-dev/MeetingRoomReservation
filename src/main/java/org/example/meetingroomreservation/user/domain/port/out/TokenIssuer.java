package org.example.meetingroomreservation.user.domain.port.out;

import org.example.meetingroomreservation.user.domain.model.IssuedToken;
import org.example.meetingroomreservation.user.domain.model.User;

/**
 * Puerto de salida de emision de tokens.
 *
 * <p>El dominio no sabe que es un JWT firmado con HS256: solo pide un token para un usuario.
 */
public interface TokenIssuer {

    IssuedToken issue(User user);
}
