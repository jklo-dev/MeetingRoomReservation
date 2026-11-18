package org.example.meetingroomreservation.reservation.domain.model;

/**
 * Reserva junto a los datos de su sala y su responsable.
 *
 * <p>El listado los necesita para cada elemento. Agruparlos aqui permite que el adaptador los
 * traiga en **una sola consulta** y evita el problema de las N+1: una pagina de 20 elementos
 * resuelta con cargas perezosas dispararia hasta 40 consultas adicionales.
 */
public record ReservationDetail(
        Reservation reservation,
        RoomSnapshot room,
        ReservationOwner owner) {
}
