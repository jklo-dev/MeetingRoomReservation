package org.example.meetingroomreservation.user.infrastructure.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.example.meetingroomreservation.user.domain.model.Role;

/**
 * Cuerpo de la peticion de creacion de usuario.
 *
 * <p>El responsable del alta es el usuario autenticado; aqui solo viajan los datos del usuario
 * que se crea. Un {@code role} fuera del enumerado lo rechaza Jackson antes de validar, y se
 * traduce a 400.
 */
record CreateUserRequest(

        @NotBlank(message = "es obligatorio")
        @Email(message = "debe ser una direccion de correo valida")
        @Size(max = 255, message = "no puede exceder 255 caracteres")
        String email,

        // El maximo de 72 no es arbitrario: BCrypt ignora en silencio todo lo que exceda esa
        // longitud, asi que aceptar mas seria enganoso.
        @NotBlank(message = "es obligatoria")
        @Size(min = 4, max = 72, message = "debe tener entre 4 y 72 caracteres")
        String password,

        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "no puede exceder 100 caracteres")
        String firstName,

        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "no puede exceder 100 caracteres")
        String lastName,

        @NotNull(message = "es obligatorio")
        Role role) {

    /**
     * Evita que la contrasena aparezca si la peticion se imprime por error.
     */
    @Override
    public String toString() {
        return "CreateUserRequest{email=" + email + ", role=" + role + "}";
    }
}
