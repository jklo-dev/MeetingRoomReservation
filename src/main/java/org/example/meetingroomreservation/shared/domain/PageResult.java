package org.example.meetingroomreservation.shared.domain;

import java.util.List;

/**
 * Pagina de resultados del dominio.
 *
 * <p>Existe para que {@code Page} de Spring Data no cruce la frontera hacia el dominio.
 *
 * @param content       elementos de la pagina solicitada
 * @param page          indice de la pagina devuelta
 * @param size          tamano de pagina aplicado
 * @param totalElements total de elementos que cumplen los filtros, ignorando la paginacion
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    public PageResult {
        content = List.copyOf(content);
    }

    public int totalPages() {
        if (totalElements == 0) {
            return 0;
        }
        return (int) ((totalElements + size - 1) / size);
    }

    public boolean first() {
        return page == 0;
    }

    public boolean last() {
        // Sin resultados, la pagina actual es tambien la ultima.
        return totalPages() == 0 || page >= totalPages() - 1;
    }
}
