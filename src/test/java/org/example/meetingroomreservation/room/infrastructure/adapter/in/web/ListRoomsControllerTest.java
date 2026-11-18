package org.example.meetingroomreservation.room.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.example.meetingroomreservation.room.domain.model.Room;
import org.example.meetingroomreservation.room.domain.model.RoomSortField;
import org.example.meetingroomreservation.room.domain.model.RoomStatus;
import org.example.meetingroomreservation.room.domain.model.RoomStatusFilter;
import org.example.meetingroomreservation.room.domain.port.in.CreateRoom;
import org.example.meetingroomreservation.room.domain.port.in.ListRooms;
import org.example.meetingroomreservation.room.domain.port.in.ListRoomsQuery;
import org.example.meetingroomreservation.shared.config.SecurityConfig;
import org.example.meetingroomreservation.shared.domain.PageResult;
import org.example.meetingroomreservation.shared.domain.SortDirection;
import org.example.meetingroomreservation.shared.web.GlobalExceptionHandler;
import org.example.meetingroomreservation.shared.web.ProblemResponseWriter;
import org.example.meetingroomreservation.support.TestJwtFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato HTTP del listado de salas. Sin base de datos: el caso de uso es un doble.
 */
@WebMvcTest(RoomController.class)
@Import({SecurityConfig.class, ProblemResponseWriter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "meetroom.security.jwt.secret=" + TestJwtFactory.SECRET)
class ListRoomsControllerTest {

    private static final Room SALA_AZUL = Room.restore(
            UUID.fromString("8c1f4e2a-7b93-4d16-a5e8-0f3c9d2b6514"),
            "Sala Azul", 12, RoomStatus.ACTIVE,
            OffsetDateTime.parse("2026-11-17T10:00:00+01:00"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListRooms listRooms;

    /** El controlador tambien depende de la creacion; aqui no se ejercita. */
    @MockitoBean
    private CreateRoom createRoom;

    private void devuelveUnaSala() {
        given(listRooms.list(any(ListRoomsQuery.class)))
                .willReturn(new PageResult<>(List.of(SALA_AZUL), 0, 20, 1));
    }

    private ListRoomsQuery queryCapturada() {
        ArgumentCaptor<ListRoomsQuery> captor = ArgumentCaptor.forClass(ListRoomsQuery.class);
        org.mockito.Mockito.verify(listRooms).list(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("200 con la envoltura de paginacion documentada")
    void devuelveLaEnvoltura() throws Exception {
        devuelveUnaSala();

        mockMvc.perform(get("/v1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id")
                        .value("8c1f4e2a-7b93-4d16-a5e8-0f3c9d2b6514"))
                .andExpect(jsonPath("$.content[0].name").value("Sala Azul"))
                .andExpect(jsonPath("$.content[0].capacity").value(12))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                // No se filtran estructuras internas de Spring Data.
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.sort").doesNotExist());
    }

    @Test
    @DisplayName("el endpoint es publico: funciona sin token")
    void esPublico() throws Exception {
        devuelveUnaSala();

        mockMvc.perform(get("/v1/rooms")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("un token valido se acepta sin ser necesario")
    void aceptaTokenValidoAunqueNoSeaNecesario() throws Exception {
        devuelveUnaSala();

        mockMvc.perform(get("/v1/rooms")
                        .header("Authorization", "Bearer " + TestJwtFactory.tokenFor(
                                UUID.randomUUID(), "USER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("un token caducado da 401 aunque la ruta sea publica")
    void tokenCaducadoDa401AunqueSeaPublica() throws Exception {
        // Comportamiento real de Spring Security, contrario a lo que supuse al redactar la spec:
        // BearerTokenAuthenticationFilter corre ANTES de la comprobacion de autorizacion. Si
        // llega una cabecera Authorization, intenta autenticarla; si falla, el entry point
        // responde 401 sin llegar nunca a evaluar el permitAll.
        //
        // Es defendible: quien envia credenciales debe enviarlas validas. Pero hay que saberlo,
        // porque significa que un token caducado rompe tambien los endpoints publicos.
        mockMvc.perform(get("/v1/rooms")
                        .header("Authorization", "Bearer " + TestJwtFactory.expiredTokenFor(
                                UUID.randomUUID(), "ADMIN")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:authentication-required"));
    }

    @Test
    @DisplayName("sin parametros aplica ACTIVE, orden por nombre ascendente, pagina 0 y tamano 20")
    void valoresPorDefecto() throws Exception {
        devuelveUnaSala();

        mockMvc.perform(get("/v1/rooms")).andExpect(status().isOk());

        ListRoomsQuery query = queryCapturada();
        assertThat(query.filter().status()).isEqualTo(RoomStatusFilter.ACTIVE);
        assertThat(query.filter().minCapacityValue()).isEmpty();
        assertThat(query.filter().nameContainsValue()).isEmpty();
        assertThat(query.sortBy()).isEqualTo(RoomSortField.NAME);
        assertThat(query.direction()).isEqualTo(SortDirection.ASC);
        assertThat(query.page().page()).isZero();
        assertThat(query.page().size()).isEqualTo(20);
    }

    @Test
    @DisplayName("los filtros llegan completos y acumulados al caso de uso")
    void losFiltrosLleganCompletos() throws Exception {
        devuelveUnaSala();

        mockMvc.perform(get("/v1/rooms")
                        .param("minCapacity", "8")
                        .param("name", "azul")
                        .param("status", "ALL")
                        .param("sortBy", "capacity")
                        .param("direction", "desc")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());

        ListRoomsQuery query = queryCapturada();
        assertThat(query.filter().minCapacityValue()).contains(8);
        assertThat(query.filter().nameContainsValue()).contains("azul");
        assertThat(query.filter().status()).isEqualTo(RoomStatusFilter.ALL);
        assertThat(query.sortBy()).isEqualTo(RoomSortField.CAPACITY);
        assertThat(query.direction()).isEqualTo(SortDirection.DESC);
        assertThat(query.page().page()).isEqualTo(2);
        assertThat(query.page().size()).isEqualTo(5);
    }

    @Test
    @DisplayName("status, sortBy y direction no distinguen mayusculas")
    void parametrosToleranMayusculas() throws Exception {
        devuelveUnaSala();

        mockMvc.perform(get("/v1/rooms")
                        .param("status", "all")
                        .param("sortBy", "CAPACITY")
                        .param("direction", "Desc"))
                .andExpect(status().isOk());

        ListRoomsQuery query = queryCapturada();
        assertThat(query.filter().status()).isEqualTo(RoomStatusFilter.ALL);
        assertThat(query.sortBy()).isEqualTo(RoomSortField.CAPACITY);
        assertThat(query.direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    @DisplayName("400 validation-error con un status fuera del enumerado, enumerando los validos")
    void rechazaStatusInvalido() throws Exception {
        mockMvc.perform(get("/v1/rooms").param("status", "BORRADA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("status"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("debe ser uno de: active, inactive, all"));
    }

    @Test
    @DisplayName("400 con un sortBy que no esta en la lista blanca")
    void rechazaSortByInvalido() throws Exception {
        mockMvc.perform(get("/v1/rooms").param("sortBy", "createdAt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("sortBy"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("debe ser uno de: name, capacity"));
    }

    @Test
    @DisplayName("400 con una direction invalida")
    void rechazaDirectionInvalida() throws Exception {
        mockMvc.perform(get("/v1/rooms").param("direction", "descendente"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("direction"));
    }

    @Test
    @DisplayName("400 con minCapacity fuera de rango")
    void rechazaMinCapacityFueraDeRango() throws Exception {
        mockMvc.perform(get("/v1/rooms").param("minCapacity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:validation-error"));

        mockMvc.perform(get("/v1/rooms").param("minCapacity", "1001"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 con minCapacity no numerico")
    void rechazaMinCapacityNoNumerico() throws Exception {
        mockMvc.perform(get("/v1/rooms").param("minCapacity", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("urn:meetroom:problem:validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("minCapacity"));
    }

    @Test
    @DisplayName("400 con page negativa o size menor que 1")
    void rechazaPaginacionInvalida() throws Exception {
        mockMvc.perform(get("/v1/rooms").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("page"));

        mockMvc.perform(get("/v1/rooms").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    @DisplayName("acepta un size muy alto: no hay maximo por decision explicita")
    void aceptaSizeMuyAlto() throws Exception {
        devuelveUnaSala();

        mockMvc.perform(get("/v1/rooms").param("size", "100000"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("200 con content vacio cuando no hay coincidencias")
    void listaVacia() throws Exception {
        given(listRooms.list(any(ListRoomsQuery.class)))
                .willReturn(new PageResult<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/v1/rooms").param("name", "no-existe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.last").value(true));
    }
}
