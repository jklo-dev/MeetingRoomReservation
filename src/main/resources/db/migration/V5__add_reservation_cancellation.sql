-- Auditoria de cancelacion. Se eligio conservar la fila en lugar de borrarla precisamente por
-- esto: sin saber cuando y quien cancelo, la conservacion aportaria poco.
--
-- cancelled_by no es "el responsable" sino "quien cancelo", que pueden ser personas distintas:
-- un administrador puede cancelar la reserva de otro. Esa es la informacion que hace util la
-- columna.
ALTER TABLE reservations
    ADD COLUMN cancelled_at timestamptz NULL,
    ADD COLUMN cancelled_by uuid NULL;

ALTER TABLE reservations
    ADD CONSTRAINT reservations_cancelled_by_fk
        FOREIGN KEY (cancelled_by) REFERENCES users (id);

-- Impide que el estado y la auditoria divergan: no se puede guardar una reserva cancelada sin
-- saber cuando ni por quien, ni una activa con marcas de cancelacion. Misma filosofia que la
-- restriccion de exclusion: la invariante en los datos, no solo en el codigo.
ALTER TABLE reservations
    ADD CONSTRAINT reservations_cancellation_consistency CHECK (
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND cancelled_by IS NOT NULL)
     OR (status = 'ACTIVE'    AND cancelled_at IS NULL     AND cancelled_by IS NULL)
    );

-- La restriccion de exclusion no se toca: ya es parcial sobre status = 'ACTIVE', asi que
-- cancelar libera el horario de forma automatica, sin reconstruir ningun indice.
