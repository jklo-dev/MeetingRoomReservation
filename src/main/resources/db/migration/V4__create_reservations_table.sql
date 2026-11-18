-- btree_gist permite meter room_id, que se compara por igualdad, en el mismo indice GiST que el
-- rango temporal. Es el unico punto de las migraciones que puede fallar por privilegios: en un
-- despliegue real puede requerir que un administrador cree la extension de antemano.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE reservations (
    id         uuid        NOT NULL,
    room_id    uuid        NOT NULL,
    owner_id   uuid        NOT NULL,
    starts_at  timestamptz NOT NULL,
    ends_at    timestamptz NOT NULL,
    attendees  integer     NOT NULL,
    status     varchar(20) NOT NULL,
    created_at timestamptz NOT NULL,

    CONSTRAINT reservations_pk PRIMARY KEY (id),
    CONSTRAINT reservations_room_fk FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT reservations_owner_fk FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT reservations_period_valid CHECK (ends_at > starts_at),
    CONSTRAINT reservations_attendees_valid CHECK (attendees >= 1),
    CONSTRAINT reservations_status_valid CHECK (status IN ('ACTIVE', 'CANCELLED'))
);

-- B-tree para la comprobacion previa de solapamiento y para los filtros del futuro listado. El
-- indice GiST de la restriccion tambien serviria, pero el B-tree es mas eficiente en consultas
-- por rango.
CREATE INDEX reservations_room_starts_idx ON reservations (room_id, starts_at);

-- Garantia real de no solapamiento: hace imposible almacenar dos reservas activas solapadas
-- venga la escritura de donde venga, incluidos scripts y correcciones manuales.
--
--   '[)'                    intervalo semiabierto: 10:00-11:00 y 11:00-12:00 NO se solapan,
--                           asi que reuniones consecutivas son validas.
--   WHERE status = 'ACTIVE' indice parcial: las reservas canceladas no bloquean el horario.
ALTER TABLE reservations
ADD CONSTRAINT reservations_no_overlap
EXCLUDE USING gist (
    room_id                              WITH =,
    tstzrange(starts_at, ends_at, '[)')  WITH &&
) WHERE (status = 'ACTIVE');
