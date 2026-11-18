package org.example.meetingroomreservation.shared.domain;

/**
 * Peticion de paginacion del dominio.
 *
 * <p>Existe para que {@code Pageable} de Spring Data no cruce la frontera hacia el dominio.
 *
 * @param page indice de pagina, empezando en 0
 * @param size elementos por pagina
 */
public record PageRequest(int page, int size) {

    public static final int DEFAULT_SIZE = 20;

    public PageRequest {
        if (page < 0) {
            throw new IllegalArgumentException("la pagina no puede ser negativa");
        }
        if (size < 1) {
            throw new IllegalArgumentException("el tamano de pagina debe ser al menos 1");
        }
        // Sin maximo, por decision explicita recogida en la deuda tecnica.
    }

    public static PageRequest firstPage() {
        return new PageRequest(0, DEFAULT_SIZE);
    }
}
