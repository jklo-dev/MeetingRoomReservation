package org.example.meetingroomreservation.room.infrastructure.adapter.out.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

import org.example.meetingroomreservation.room.domain.model.RoomFilter;
import org.springframework.data.jpa.domain.Specification;

/**
 * Traduce los criterios del dominio a condiciones de consulta.
 */
final class RoomSpecifications {

    /** Caracter de escape para los comodines de LIKE. */
    private static final char ESCAPE = '\\';

    private RoomSpecifications() {
    }

    /**
     * Compone los filtros presentes en una unica especificacion.
     *
     * <p>Se acumulan predicados en una lista y se combinan con {@code cb.and(...)} en lugar de
     * usar {@code Specification.allOf(...)}: ese metodo rechaza elementos nulos, asi que pasarle
     * un hueco por cada filtro ausente fallaba con "Other specification must not be null" en el
     * caso mas comun de todos, una peticion sin parametros.
     *
     * <p>{@code cb.and(...)} sin predicados devuelve una conjuncion siempre verdadera, que es
     * exactamente lo que debe ocurrir cuando no hay filtros: devolver todo.
     */
    static Specification<RoomJpaEntity> from(RoomFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            filter.minCapacityValue().ifPresent(min ->
                    predicados.add(cb.greaterThanOrEqualTo(root.get("capacity"), min)));

            if (filter.status().appliesFilter()) {
                predicados.add(cb.equal(root.get("status"), filter.status().asStatus()));
            }

            filter.nameContainsValue().ifPresent(texto ->
                    predicados.add(cb.like(
                            cb.lower(root.get("name")),
                            "%" + escapeWildcards(texto.toLowerCase(Locale.ROOT)) + "%",
                            ESCAPE)));

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    /**
     * Neutraliza los comodines de LIKE para que se busquen como texto literal.
     *
     * <p>Sin esto, buscar {@code %} devolveria todas las salas y {@code _} actuaria como
     * cualquier caracter: el cliente tendria acceso involuntario a la sintaxis del patron.
     */
    static String escapeWildcards(String texto) {
        return texto
                .replace(String.valueOf(ESCAPE), ESCAPE + "" + ESCAPE)
                .replace("%", ESCAPE + "%")
                .replace("_", ESCAPE + "_");
    }
}
