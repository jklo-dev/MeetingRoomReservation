CREATE TABLE rooms (
    id         uuid         NOT NULL,
    name       varchar(100) NOT NULL,
    capacity   integer      NOT NULL,
    status     varchar(20)  NOT NULL,
    created_at timestamptz  NOT NULL,

    CONSTRAINT rooms_pk PRIMARY KEY (id),
    CONSTRAINT rooms_capacity_valid CHECK (capacity BETWEEN 1 AND 1000),
    CONSTRAINT rooms_status_valid CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- Indice sobre lower(name) y no sobre name: asi 'Sala Azul' y 'sala azul' entran en conflicto,
-- mientras la columna conserva la capitalizacion original para mostrarla. Los espacios externos
-- se recortan antes de guardar, por lo que no hacen falta en la expresion.
CREATE UNIQUE INDEX rooms_name_unique_ci ON rooms (lower(name));
