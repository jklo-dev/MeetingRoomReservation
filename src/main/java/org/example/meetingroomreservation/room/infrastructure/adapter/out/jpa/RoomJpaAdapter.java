package org.example.meetingroomreservation.room.infrastructure.adapter.out.jpa;

import org.example.meetingroomreservation.room.domain.exception.RoomNameAlreadyExistsException;
import org.example.meetingroomreservation.room.domain.model.Room;
import org.example.meetingroomreservation.room.domain.port.out.RoomRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
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
