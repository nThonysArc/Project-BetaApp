-- ============================================================
-- V8: Variedad y Kato pasan de texto libre a catalogo real
--
-- Antes: corte_variedad_detalle.variedad, materia_prima_plan.variedad y
-- corte_maquina_kato.kato_nombre eran VARCHAR libres, escritos a mano en
-- cada corte - propenso a duplicados/errores de tipeo (ej. "Maquina 1"
-- vs "Maqna 1") y sin posibilidad de reutilizar el catalogo en futuras
-- aplicaciones.
--
-- Ahora: Variedad es un catalogo por producto (igual patron que Campana),
-- y MaquinaKato es un catalogo por maquina (definido una sola vez al
-- crear/editar la maquina). El corte referencia ambos por FK, nunca
-- vuelve a escribirlos como texto libre.
--
-- Esta migracion vacia las tablas de datos de prueba afectadas
-- (corte_variedad_detalle, corte_maquina_kato, corte, materia_prima_plan)
-- porque en este punto del desarrollo son datos descartables, no
-- produccion real. Reconstruir el modelo correctamente ahora es mas
-- simple que migrar texto libre a catalogo automaticamente.
-- ============================================================

-- ---- Limpieza de datos de prueba afectados por el cambio de esquema ----
DELETE FROM corte_maquina_kato;
DELETE FROM corte_variedad_detalle;
DELETE FROM auditoria_edicion WHERE entidad = 'CORTE';
DELETE FROM corte;
DELETE FROM materia_prima_plan;

-- ============================================
-- VARIEDAD (catalogo, scoped por producto)
-- ============================================
CREATE TABLE variedad (
    id              BIGSERIAL PRIMARY KEY,
    producto_id     BIGINT NOT NULL REFERENCES producto(id),
    nombre          VARCHAR(80) NOT NULL,
    orden           SMALLINT NOT NULL DEFAULT 0,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT REFERENCES usuario(id),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT REFERENCES usuario(id),
    CONSTRAINT uq_variedad_producto_nombre UNIQUE (producto_id, nombre)
);

CREATE INDEX idx_variedad_producto ON variedad(producto_id);

CREATE TRIGGER trg_variedad_updated_at BEFORE UPDATE ON variedad
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================
-- MAQUINA_KATO (catalogo, scoped por maquina - se define al crear/editar
-- la maquina, no se vuelve a escribir en cada corte)
-- ============================================
CREATE TABLE maquina_kato (
    id              BIGSERIAL PRIMARY KEY,
    maquina_id      BIGINT NOT NULL REFERENCES maquina(id),
    nombre          VARCHAR(30) NOT NULL,
    orden           SMALLINT NOT NULL DEFAULT 0,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT REFERENCES usuario(id),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT REFERENCES usuario(id),
    CONSTRAINT uq_maquina_kato_maquina_nombre UNIQUE (maquina_id, nombre)
);

CREATE INDEX idx_maquina_kato_maquina ON maquina_kato(maquina_id);

CREATE TRIGGER trg_maquina_kato_updated_at BEFORE UPDATE ON maquina_kato
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================
-- corte_variedad_detalle: variedad texto libre -> FK a catalogo
-- ============================================
ALTER TABLE corte_variedad_detalle DROP COLUMN variedad;
ALTER TABLE corte_variedad_detalle ADD COLUMN variedad_id BIGINT REFERENCES variedad(id);
ALTER TABLE corte_variedad_detalle ALTER COLUMN variedad_id SET NOT NULL;

CREATE INDEX idx_cvd_variedad ON corte_variedad_detalle(variedad_id);

-- ============================================
-- corte_maquina_kato: kato_nombre texto libre -> FK a catalogo
-- ============================================
ALTER TABLE corte_maquina_kato DROP COLUMN kato_nombre;
ALTER TABLE corte_maquina_kato ADD COLUMN maquina_kato_id BIGINT REFERENCES maquina_kato(id);
ALTER TABLE corte_maquina_kato ALTER COLUMN maquina_kato_id SET NOT NULL;

CREATE INDEX idx_cmk_maquina_kato ON corte_maquina_kato(maquina_kato_id);

-- ============================================
-- materia_prima_plan: variedad texto libre -> FK a catalogo
-- ============================================
ALTER TABLE materia_prima_plan DROP COLUMN variedad;
ALTER TABLE materia_prima_plan ADD COLUMN variedad_id BIGINT REFERENCES variedad(id);
ALTER TABLE materia_prima_plan ALTER COLUMN variedad_id SET NOT NULL;
ALTER TABLE materia_prima_plan ADD CONSTRAINT uq_mp_plan_proceso_variedad UNIQUE (proceso_diario_id, variedad_id);

CREATE INDEX idx_mp_plan_variedad ON materia_prima_plan(variedad_id);
