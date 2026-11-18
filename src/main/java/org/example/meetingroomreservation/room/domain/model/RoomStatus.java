package org.example.meetingroomreservation.room.domain.model;

/**
 * Estado de una sala.
 *
 * <p>{@code INACTIVE} existe desde el principio aunque la creacion no pueda producirlo: la
 * columna necesita su restriccion y anadirlo despues obligaria a migrar el CHECK sin ninguna
 * ganancia. La desactivacion llegara en su propia funcionalidad.
 */
public enum RoomStatus {
    ACTIVE,
    INACTIVE
}
