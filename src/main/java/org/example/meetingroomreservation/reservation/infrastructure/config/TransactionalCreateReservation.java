package org.example.meetingroomreservation.reservation.infrastructure.config;

import java.time.Clock;

import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservation;
import org.example.meetingroomreservation.reservation.domain.port.in.CreateReservationCommand;
import org.example.meetingroomreservation.reservation.domain.port.out.OwnerLookupPort;
import org.example.meetingroomreservation.reservation.domain.port.out.ReservationRepositoryPort;
import org.example.meetingroomreservation.reservation.domain.port.out.RoomCalendarPort;
import org.example.meetingroomreservation.reservation.domain.service.CreateReservationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Envuelve el caso de uso en una transaccion.
 *
 * <p>La transaccion es una preocupacion de infraestructura, asi que {@code @Transactional} vive
 * aqui y no en {@link CreateReservationService}, que sigue sin conocer Spring. El decorador
 * construye el servicio de dominio con {@code new} para que exista un unico bean de
 * {@link CreateReservation} y el proxy transaccional se aplique de verdad.
 *
 * <p><b>Esto no es decorativo</b>: el cerrojo que toma el adaptador del calendario solo existe
 * mientras la transaccion vive. Si la comprobacion de solapamiento y la insercion cayeran en
 * transacciones distintas, el cerrojo se liberaria entre medias y no protegeria nada.
 */
@Component
class TransactionalCreateReservation implements CreateReservation {

    private final CreateReservationService delegate;

    TransactionalCreateReservation(RoomCalendarPort roomCalendar,
                                   ReservationRepositoryPort reservations,
                                   OwnerLookupPort owners,
                                   Clock clock) {
        this.delegate = new CreateReservationService(roomCalendar, reservations, owners, clock);
    }

    /**
     * El aislamiento se declara de forma explicita aunque {@code READ_COMMITTED} ya sea el valor
     * por defecto: el patron de cerrojo depende de el. Con {@code REPEATABLE_READ}, la
     * instantanea de la transaccion que espera seria anterior al commit de la primera y no veria
     * su reserva; el {@code FOR UPDATE} detectaria el conflicto y lanzaria un error de
     * serializacion en lugar del 409 esperado.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Result create(CreateReservationCommand command) {
        return delegate.create(command);
    }
}
