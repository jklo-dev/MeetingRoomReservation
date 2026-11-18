package org.example.meetingroomreservation.room.domain.model;

/**
 * Campos por los que se permite ordenar el listado de salas.
 *
 * <p>Es una lista blanca por diseno: un enumerado impide que el cliente ordene por cualquier
 * atributo del modelo interno.
 */
public enum RoomSortField {

    NAME,
    CAPACITY;

    public static final RoomSortField DEFAULT = NAME;
}
