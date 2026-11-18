package org.example.meetingroomreservation.room.infrastructure.adapter.out.jpa;

import org.example.meetingroomreservation.room.domain.exception.RoomNameAlreadyExistsException;
import org.example.meetingroomreservation.room.domain.model.Room;
import org.example.meetingroomreservation.room.domain.port.in.ListRoomsQuery;
import org.example.meetingroomreservation.room.domain.port.out.RoomRepositoryPort;
import org.example.meetingroomreservation.shared.domain.PageResult;
import org.example.meetingroomreservation.shared.domain.SortDirection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Adaptador de persistencia que satisface {@link RoomRepositoryPort}.
 */
@Component
class RoomJpaAdapter implements RoomRepositoryPort {

    private final RoomJpaRepository repository;

    RoomJpaAdapter(RoomJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByNormalizedName(String normalizedName) {
        return repository.existsByNormalizedName(normalizedName);
    }

    @Override
    public PageResult<Room> search(ListRoomsQuery query) {
        Page<RoomJpaEntity> pagina = repository.findAll(
                RoomSpecifications.from(query.filter()),
                org.springframework.data.domain.PageRequest.of(
                        query.page().page(),
                        query.page().size(),
                        sortOf(query)));

        return new PageResult<>(
                pagina.getContent().stream().map(RoomJpaEntity::toDomain).toList(),
                query.page().page(),
                query.page().size(),
                pagina.getTotalElements());
    }

    private Sort sortOf(ListRoomsQuery query) {
        boolean ascendente = query.direction() == SortDirection.ASC;

        Sort principal = switch (query.sortBy()) {
            // ignoreCase genera lower(name) en el ORDER BY, de modo que el orden no dependa de
            // la configuracion regional de la base de datos.
            case NAME -> Sort.by(order("name", ascendente).ignoreCase());
            case CAPACITY -> Sort.by(order("capacity", ascendente));
        };

        // Desempate obligatorio: sin el, dos salas con la misma capacidad podrian repartirse de
        // forma inconsistente entre paginas y aparecer duplicadas u omitidas al paginar.
        return principal.and(Sort.by(Sort.Order.asc("id")));
    }

    private Sort.Order order(String property, boolean ascendente) {
        return ascendente ? Sort.Order.asc(property) : Sort.Order.desc(property);
    }

    @Override
    public Room save(Room room) {
        try {
            return repository.save(RoomJpaEntity.fromDomain(room)).toDomain();
        } catch (DataIntegrityViolationException e) {
            // La comprobacion previa no cierra la ventana entre dos peticiones simultaneas con
            // el mismo nombre: el indice unico es quien decide, y su violacion se traduce a la
            // misma excepcion de dominio para que el cliente reciba un 409.
            throw new RoomNameAlreadyExistsException();
        }
    }
}
