package org.example.meetingroomreservation.reservation.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import org.example.meetingroomreservation.reservation.domain.model.RoomSnapshot;

/**
 * Acceso **en exclusiva** al calendario de una sala.
 *
 * <p>Esta expresado en vocabulario de dominio —«dame el calendario de esta sala en exclusiva»— y
 * no de persistencia. El dominio no sabe nada de {@code SELECT ... FOR UPDATE}: es el adaptador
 * el que decide que la forma de cumplir esa promesa es un cerrojo de fila. Si el mecanismo
 * cambiara, el dominio no cambiaria.
 */
public interface RoomCalendarPort {

    /**
     * Reserva el calendario de la sala para la transaccion en curso y devuelve sus datos.
     *
     * <p>Debe invocarse **antes** de cualquier consulta de disponibilidad: al reves, la ventana
     * de carrera sigue abierta. El acceso exclusivo se mantiene hasta que la transaccion termina.
     *
     * @return vacio si la sala no existe
     * @throws org.example.meetingroomreservation.reservation.domain.exception.ReservationExceptions.RoomBusy
     *         si no se consigue el acceso exclusivo en el tiempo maximo de espera
     */
    Optional<RoomSnapshot> lockAndGet(UUID roomId);

    /**
     * Lee los datos de la sala **sin** tomar ningun cerrojo.
     *
     * <p>Lo usan las operaciones que no deciden nada en funcion de la disponibilidad, como la
     * cancelacion: afectan a una fila identificada por clave primaria y solo necesitan el nombre
     * y la capacidad de la sala para componer la respuesta. Tomar el cerrojo ahi seria un coste
     * sin ninguna garantia a cambio.
     */
    Optional<RoomSnapshot> findWithoutLock(UUID roomId);
}
