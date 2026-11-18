package org.example.meetingroomreservation.room.domain.model;

/**
 * Filtro de estado del listado de salas.
 *
 * <p>No es el mismo enumerado que {@link RoomStatus}: aqui existe {@code ALL}, que no es un
 * estado posible de una sala sino la ausencia de filtro.
 */
public enum RoomStatusFilter {

    ACTIVE,
    INACTIVE,
    ALL;

    /** Valor por defecto: sin parametros, el listado devuelve solo las salas reservables. */
    public static final RoomStatusFilter DEFAULT = ACTIVE;

    public boolean appliesFilter() {
        return this != ALL;
    }

    /**
     * @throws IllegalStateException si se invoca sobre {@code ALL}, que no corresponde a ningun
     *         estado concreto
     */
    public RoomStatus asStatus() {
        if (this == ALL) {
            throw new IllegalStateException("ALL no corresponde a un estado concreto");
        }
        return RoomStatus.valueOf(name());
    }
}
