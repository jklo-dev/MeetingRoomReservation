package org.example.meetingroomreservation.shared.web;

import java.util.List;
import java.util.function.Function;

import org.example.meetingroomreservation.shared.domain.PageResult;

/**
 * Envoltura de respuesta paginada, con metadatos planos.
 *
 * <p>Es un DTO propio y no el {@code Page} de Spring Data: su JSON incluye estructuras internas
 * como {@code pageable} y {@code sort}, y Spring desaconseja expresamente serializarlo porque no
 * es un contrato estable entre versiones.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <D, R> PageResponse<R> from(PageResult<D> result, Function<D, R> mapper) {
        return new PageResponse<>(
                result.content().stream().map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.first(),
                result.last());
    }
}
