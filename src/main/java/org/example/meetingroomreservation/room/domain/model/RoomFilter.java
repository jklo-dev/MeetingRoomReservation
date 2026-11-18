package org.example.meetingroomreservation.room.domain.model;

import java.util.Optional;

/**
 * Criterios de busqueda del listado de salas. Todos los filtros son opcionales y acumulativos.
 *
 * @param minCapacity capacidad minima, inclusive
 * @param status      filtro de estado
 * @param nameContains texto que debe contener el nombre, sin distinguir mayusculas
 */
public record RoomFilter(Integer minCapacity, RoomStatusFilter status, String nameContains) {

    public RoomFilter {
        if (status == null) {
            status = RoomStatusFilter.DEFAULT;
        }
        if (minCapacity != null
                && (minCapacity < Room.MIN_CAPACITY || minCapacity > Room.MAX_CAPACITY)) {
            throw new IllegalArgumentException("la capacidad minima debe estar entre "
                    + Room.MIN_CAPACITY + " y " + Room.MAX_CAPACITY);
        }
        // Un texto en blanco se trata como si no se hubiera enviado el filtro.
        if (nameContains != null) {
            String trimmed = nameContains.trim();
            nameContains = trimmed.isEmpty() ? null : trimmed;
        }
    }

    public static RoomFilter none() {
        return new RoomFilter(null, RoomStatusFilter.DEFAULT, null);
    }

    public Optional<Integer> minCapacityValue() {
        return Optional.ofNullable(minCapacity);
    }

    public Optional<String> nameContainsValue() {
        return Optional.ofNullable(nameContains);
    }
}
