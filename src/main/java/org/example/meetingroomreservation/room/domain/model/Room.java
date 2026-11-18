package org.example.meetingroomreservation.room.domain.model;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Sala de reuniones.
 *
 * <p>Garantiza sus invariantes al construirse: nombre no vacio tras recortar y capacidad dentro
 * del rango permitido.
 */
public final class Room {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MIN_CAPACITY = 1;
    public static final int MAX_CAPACITY = 1000;

    private final UUID id;
    private final String name;
    private final int capacity;
    private final RoomStatus status;
    private final OffsetDateTime createdAt;

    private Room(UUID id, String name, int capacity, RoomStatus status, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Crea una sala nueva, generando su identificador en el dominio. Toda sala nace activa.
     */
    public static Room create(String name, int capacity, OffsetDateTime createdAt) {
        return restore(UUID.randomUUID(), name, capacity, RoomStatus.ACTIVE, createdAt);
    }

    /**
     * Reconstruye una sala existente, por ejemplo al leerla de la persistencia.
     */
    public static Room restore(UUID id, String name, int capacity, RoomStatus status,
                               OffsetDateTime createdAt) {
        Objects.requireNonNull(id, "el identificador es obligatorio");
        Objects.requireNonNull(status, "el estado es obligatorio");
        Objects.requireNonNull(createdAt, "el instante de creacion es obligatorio");

        String trimmedName = requireName(name);
        requireCapacity(capacity);

        return new Room(id, trimmedName, capacity, status, createdAt);
    }

    /**
     * Forma normalizada del nombre, usada **solo** para comparar unicidad.
     *
     * <p>El valor almacenado conserva la capitalizacion original porque es texto de
     * presentacion; lo que se compara es esta forma. Debe coincidir exactamente con la expresion
     * del indice unico en base de datos, que es {@code lower(name)}.
     */
    public static String normalizedName(String name) {
        return requireName(name).toLowerCase(Locale.ROOT);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("el nombre de la sala es obligatorio");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "el nombre de la sala no puede exceder " + MAX_NAME_LENGTH + " caracteres");
        }
        return trimmed;
    }

    private static void requireCapacity(int capacity) {
        if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("la capacidad debe estar entre " + MIN_CAPACITY
                    + " y " + MAX_CAPACITY);
        }
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int capacity() {
        return capacity;
    }

    public RoomStatus status() {
        return status;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    public boolean isActive() {
        return status == RoomStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return "Room{id=" + id + ", name=" + name + ", capacity=" + capacity
                + ", status=" + status + "}";
    }
}
