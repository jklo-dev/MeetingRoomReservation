package org.example.meetingroomreservation.reservation.domain.model;

/**
 * Filtro de estado del listado de reservas.
 *
 * <p>No es el mismo enumerado que {@link ReservationStatus}: aqui existe {@code ALL}, que no es
 * un estado posible de una reserva sino la ausencia de filtro.
 */
public enum ReservationStatusFilter {

    ACTIVE,
    CANCELLED,
    ALL;

    /** Sin parametros, el listado devuelve solo las reservas vigentes. */
    public static final ReservationStatusFilter DEFAULT = ACTIVE;

    public boolean appliesFilter() {
        return this != ALL;
    }

    public ReservationStatus asStatus() {
        if (this == ALL) {
            throw new IllegalStateException("ALL no corresponde a un estado concreto");
        }
        return ReservationStatus.valueOf(name());
    }
}
