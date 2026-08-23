-- ============================================================
-- V9: Unifica variedad + maquina/kato en una sola fila de detalle
--
-- Hasta ahora "variedades" (variedad, jabas, peso por viaje) y
-- "maquinas/katos" (maquina, kato, empacadores, kg/empacador) eran dos
-- tablas independientes dentro de un corte. Esto no permitia saber
-- cuantos kilos proceso especificamente cada kato de cada maquina - el
-- peso solo se totalizaba a nivel de variedad, agregado para todo el
-- corte.
--
-- Ahora cada fila de detalle es: maquina + kato + variedad + jabas +
-- peso total (jabas * peso promedio de la variedad, segun el ultimo
-- ingreso reportado) + empacadores + kg por empacador (peso total /
-- empacadores). Con esto si se puede saber cuanto proceso cada kato.
--
-- Se vacian corte/corte_variedad_detalle/corte_maquina_kato: son datos
-- de prueba de la etapa anterior, no produccion real.
-- ============================================================

DELETE FROM auditoria_edicion WHERE entidad = 'CORTE';
DROP TABLE IF EXISTS corte_variedad_detalle;
DROP TABLE IF EXISTS corte_maquina_kato;

CREATE TABLE corte_detalle (
    id                      BIGSERIAL PRIMARY KEY,
    corte_id                BIGINT NOT NULL REFERENCES corte(id) ON DELETE CASCADE,
    maquina_id              BIGINT NOT NULL REFERENCES maquina(id),
    maquina_kato_id         BIGINT NOT NULL REFERENCES maquina_kato(id),
    variedad_id             BIGINT NOT NULL REFERENCES variedad(id),
    jabas                   NUMERIC(10,0) NOT NULL DEFAULT 0 CHECK (jabas >= 0),
    peso_total              NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (peso_total >= 0),
    empacadores             SMALLINT NOT NULL DEFAULT 0 CHECK (empacadores >= 0),
    kg_por_empacador        NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (kg_por_empacador >= 0),
    orden                   SMALLINT NOT NULL DEFAULT 0,
    cliente_uuid            UUID UNIQUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by              BIGINT REFERENCES usuario(id),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by              BIGINT REFERENCES usuario(id)
);

CREATE INDEX idx_corte_detalle_corte ON corte_detalle(corte_id);
CREATE INDEX idx_corte_detalle_maquina ON corte_detalle(maquina_id);
CREATE INDEX idx_corte_detalle_maquina_kato ON corte_detalle(maquina_kato_id);
CREATE INDEX idx_corte_detalle_variedad ON corte_detalle(variedad_id);

CREATE TRIGGER trg_corte_detalle_updated_at BEFORE UPDATE ON corte_detalle
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
