package org.example.meetingroomreservation.room.infrastructure.adapter.in.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de la peticion de creacion de sala.
 *
 * <p>No acepta el estado: toda sala nace activa. Si llega un campo {@code status}, Jackson lo
 * ignora.
 */
record CreateRoomRequest(

        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "no puede exceder 100 caracteres")
        String name,

        // Integer y no int: con int, un campo ausente llegaria como 0 y el mensaje de error
        // hablaria de rango en lugar de decir que falta.
        @NotNull(message = "es obligatoria")
        @Min(value = 1, message = "debe estar entre 1 y 1000")
        @Max(value = 1000, message = "debe estar entre 1 y 1000")
        Integer capacity) {
}
