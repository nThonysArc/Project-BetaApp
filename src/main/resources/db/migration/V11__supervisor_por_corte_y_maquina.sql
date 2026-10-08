-- ============================================================
-- V11: El supervisor pasa a registrarse por CORTE y MAQUINA
--
-- Hasta ahora el supervisor era un dato fijo de la maquina
-- (maquina.supervisor_id). En la practica el supervisor puede cambiar
-- de maquina a mitad del dia (por ejemplo de las 07:00 a las 10:00
-- esta en una maquina y de las 10:00 al mediodia en otra), y cambiar
-- el supervisor de la maquina reescribia el historial de todos los
-- cortes anteriores.
--
-- Ahora cada corte guarda quien superviso cada maquina en esa hora:
-- corte_maquina (corte + maquina -> supervisor). Se permite UN solo
-- supervisor por maquina y por corte (UNIQUE corte_id + maquina_id).
--
-- maquina.supervisor_id se conserva, pero solo como valor sugerido
-- para el primer corte del dia; los cortes siguientes copian el del
-- corte anterior.
--
-- Los cortes ya existentes se rellenan con el supervisor que tenia
-- asignada la maquina, para que el historial no quede vacio.
-- ============================================================

CREATE TABLE corte_maquina (
    id              BIGSERIAL PRIMARY KEY,
    corte_id        BIGINT NOT NULL REFERENCES corte(id) ON DELETE CASCADE,
    maquina_id      BIGINT NOT NULL REFERENCES maquina(id),
    supervisor_id   BIGINT REFERENCES usuario(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT REFERENCES usuario(id),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT REFERENCES usuario(id),
    CONSTRAINT uq_corte_maquina UNIQUE (corte_id, maquina_id)
);

CREATE INDEX idx_corte_maquina_corte ON corte_maquina(corte_id);
CREATE INDEX idx_corte_maquina_maquina ON corte_maquina(maquina_id);
CREATE INDEX idx_corte_maquina_supervisor ON corte_maquina(supervisor_id);

CREATE TRIGGER trg_corte_maquina_updated_at BEFORE UPDATE ON corte_maquina
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Relleno del historial existente
INSERT INTO corte_maquina (corte_id, maquina_id, supervisor_id)
SELECT DISTINCT cd.corte_id, cd.maquina_id, m.supervisor_id
FROM corte_detalle cd
JOIN maquina m ON m.id = cd.maquina_id;
