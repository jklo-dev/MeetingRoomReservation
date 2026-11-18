package org.example.meetingroomreservation.user.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales de la peticion de login.
 *
 * <p>Deliberadamente solo se valida la **presencia** de los campos, a diferencia de la creacion
 * de usuarios. No se comprueba el formato del correo ni la longitud de la contrasena por dos
 * razones: las reglas de formato pueden endurecerse con el tiempo y un usuario creado bajo
 * reglas antiguas debe poder seguir autenticandose; y un correo mal formado no es una peticion
 * invalida sino una credencial que no existe, cuya respuesta correcta es 401 y no 400.
 */
record LoginRequest(

        @NotBlank(message = "es obligatorio")
        String email,

        @NotBlank(message = "es obligatoria")
        String password) {

    /**
     * La especificacion exige que el cuerpo del login no se registre en ningun nivel de log.
     */
    @Override
    public String toString() {
        return "LoginRequest{email=" + email + "}";
    }
}
