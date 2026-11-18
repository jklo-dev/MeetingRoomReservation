package org.example.meetingroomreservation.reservation.infrastructure.config;

import org.example.meetingroomreservation.reservation.domain.port.in.ListReservations;
import org.example.meetingroomreservation.reservation.domain.port.out.ReservationRepositoryPort;
import org.example.meetingroomreservation.reservation.domain.service.ListReservationsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado de las consultas de reservas.
 *
 * <p>El listado no necesita decorador transaccional: es una lectura y se resuelve en dos
 * consultas independientes, sin invariante que proteger entre ellas.
 */
@Configuration
class ReservationQueryConfig {

    @Bean
    ListReservations listReservations(ReservationRepositoryPort reservations) {
        return new ListReservationsService(reservations);
    }
}
