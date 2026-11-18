package org.example.meetingroomreservation.room.infrastructure.adapter.out.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.example.meetingroomreservation.room.domain.model.RoomFilter;
import org.example.meetingroomreservation.room.domain.model.RoomStatusFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Composicion de filtros sin base de datos, con dobles de la API de criterios.
 *
 * <p>Cubre una regresion concreta: una peticion sin parametros debe producir una consulta sin
 * condiciones, y no fallar. La version anterior usaba {@code Specification.allOf(...)} con
 * huecos nulos y reventaba con "Other specification must not be null" justo en ese caso, el mas
 * comun de todos. Ningun test lo detecto porque los de capa web sustituyen el caso de uso por un
 * doble y nunca llegan hasta aqui.
 */
class RoomSpecificationsTest {

    @SuppressWarnings("unchecked")
    private Predicate[] predicadosDe(RoomFilter filter) {
        Root<RoomJpaEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path<Object> path = mock(Path.class);
        Expression<String> lower = mock(Expression.class);
        Predicate predicado = mock(Predicate.class);
        Predicate conjuncion = mock(Predicate.class);

        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(cb.lower(any())).thenReturn(lower);
        lenient().when(cb.greaterThanOrEqualTo(any(), anyInt())).thenReturn(predicado);
        lenient().when(cb.equal(any(), any())).thenReturn(predicado);
        lenient().when(cb.like(any(), anyString(), anyChar())).thenReturn(predicado);
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(conjuncion);

        RoomSpecifications.from(filter).toPredicate(root, query, cb);

        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        org.mockito.Mockito.verify(cb).and(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("sin filtros no genera ninguna condicion y no falla")
    void sinFiltrosNoGeneraCondiciones() {
        // RoomFilter.none() lleva status ACTIVE por defecto, que si es un filtro.
        RoomFilter todas = new RoomFilter(null, RoomStatusFilter.ALL, null);

        assertThat(predicadosDe(todas)).isEmpty();
    }

    @Test
    @DisplayName("el estado por defecto, ACTIVE, genera una condicion")
    void estadoPorDefectoGeneraCondicion() {
        assertThat(predicadosDe(RoomFilter.none())).hasSize(1);
    }

    @Test
    @DisplayName("ALL no genera condicion de estado")
    void todosNoGeneraCondicionDeEstado() {
        RoomFilter filtro = new RoomFilter(null, RoomStatusFilter.ALL, null);

        assertThat(predicadosDe(filtro)).isEmpty();
    }

    @Test
    @DisplayName("los tres filtros juntos generan tres condiciones acumuladas")
    void tresFiltrosGeneranTresCondiciones() {
        RoomFilter filtro = new RoomFilter(8, RoomStatusFilter.ACTIVE, "azul");

        assertThat(predicadosDe(filtro)).hasSize(3);
    }

    @Test
    @DisplayName("un nombre en blanco no genera condicion")
    void nombreEnBlancoNoGeneraCondicion() {
        RoomFilter filtro = new RoomFilter(null, RoomStatusFilter.ALL, "   ");

        assertThat(predicadosDe(filtro)).isEmpty();
    }

    @Test
    @DisplayName("los comodines de LIKE se escapan para buscarse como texto literal")
    void escapaComodines() {
        // Sin esto, buscar % devolveria todas las salas y _ actuaria como cualquier caracter.
        assertThat(RoomSpecifications.escapeWildcards("100%")).isEqualTo("100\\%");
        assertThat(RoomSpecifications.escapeWildcards("sala_1")).isEqualTo("sala\\_1");
        assertThat(RoomSpecifications.escapeWildcards("a\\b")).isEqualTo("a\\\\b");
        assertThat(RoomSpecifications.escapeWildcards("azul")).isEqualTo("azul");
    }
}
