-- ============================================================
-- V1: Esquema base
-- Sistema de avance de produccion por hora - Beta Agroindustrial
-- Multi-producto desde el diseno (arandano, esparrago, uva, granada...)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- ENUMS
-- ============================================
CREATE TYPE rol_usuario AS ENUM ('ADMIN', 'SUPERVISOR', 'OPERADOR');
CREATE TYPE estado_proceso AS ENUM ('ABIERTO', 'CERRADO');

-- ============================================
-- USUARIO
-- ============================================
CREATE TABLE usuario (
    id              BIGSERIAL PRIMARY KEY,
    nombre_completo VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    rol             rol_usuario NOT NULL,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_usuario_rol ON usuario(rol) WHERE activo = TRUE;

-- ============================================
-- PRODUCTO (catalogo: arandano, esparrago, uva, granada...)
-- ============================================
CREATE TABLE producto (
    id                  BIGSERIAL PRIMARY KEY,
    nombre              VARCHAR(80) NOT NULL UNIQUE,
    unidad_medida       VARCHAR(20) NOT NULL DEFAULT 'KG',
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================
-- CAMPANA (ej: "Arandanos 2026")
-- ============================================
CREATE TABLE campana (
    id              BIGSERIAL PRIMARY KEY,
    producto_id     BIGINT NOT NULL REFERENCES producto(id),
    nombre          VARCHAR(120) NOT NULL,
    anio            SMALLINT NOT NULL,
    fecha_inicio    DATE NOT NULL,
    fecha_fin       DATE,
    activa          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_campana_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

CREATE INDEX idx_campana_producto ON campana(producto_id);

-- Solo 1 campana activa por producto a la vez.
-- Quitar este indice si en el futuro se requieren campanas simultaneas del mismo producto.
CREATE UNIQUE INDEX uq_campana_activa_por_producto
    ON campana(producto_id) WHERE activa = TRUE;

-- ============================================
-- MAQUINA (definida por campana, con su supervisor)
-- ============================================
CREATE TABLE maquina (
    id              BIGSERIAL PRIMARY KEY,
    campana_id      BIGINT NOT NULL REFERENCES campana(id),
    nombre          VARCHAR(50) NOT NULL,
    supervisor_id   BIGINT REFERENCES usuario(id),
    orden           SMALLINT NOT NULL DEFAULT 0,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_maquina_campana_nombre UNIQUE (campana_id, nombre)
);

CREATE INDEX idx_maquina_campana ON maquina(campana_id);

-- ============================================
-- PROCESO_DIARIO (un dia de proceso dentro de una campana)
-- ============================================
CREATE TABLE proceso_diario (
    id              BIGSERIAL PRIMARY KEY,
    campana_id      BIGINT NOT NULL REFERENCES campana(id),
    fecha           DATE NOT NULL,
    estado          estado_proceso NOT NULL DEFAULT 'ABIERTO',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_proceso_campana_fecha UNIQUE (campana_id, fecha)
);

CREATE INDEX idx_proceso_fecha ON proceso_diario(fecha);
CREATE INDEX idx_proceso_campana ON proceso_diario(campana_id);

-- ============================================
-- MATERIA_PRIMA_PLAN ("TOTAL A PROCESAR" por variedad, planificado del dia)
-- ============================================
CREATE TABLE materia_prima_plan (
    id                  BIGSERIAL PRIMARY KEY,
    proceso_diario_id   BIGINT NOT NULL REFERENCES proceso_diario(id) ON DELETE CASCADE,
    variedad            VARCHAR(80) NOT NULL,
    jabas_plan          NUMERIC(10,0) NOT NULL DEFAULT 0 CHECK (jabas_plan >= 0),
    kilos_plan          NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (kilos_plan >= 0),
    orden               SMALLINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_mp_plan_proceso ON materia_prima_plan(proceso_diario_id);

-- ============================================
-- CORTE (registro por hora - tabla central del sistema)
-- ============================================
CREATE TABLE corte (
    id                  BIGSERIAL PRIMARY KEY,
    proceso_diario_id   BIGINT NOT NULL REFERENCES proceso_diario(id) ON DELETE CASCADE,
    numero_corte        SMALLINT NOT NULL,
    hora_inicio         TIME NOT NULL,
    hora_fin            TIME NOT NULL,
    fecha_cosecha       DATE,
    observacion         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          BIGINT REFERENCES usuario(id),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by          BIGINT REFERENCES usuario(id),
    CONSTRAINT uq_corte_proceso_numero UNIQUE (proceso_diario_id, numero_corte),
    CONSTRAINT chk_corte_horas CHECK (hora_fin > hora_inicio)
);

CREATE INDEX idx_corte_proceso ON corte(proceso_diario_id);

-- ============================================
-- CORTE_VARIEDAD_DETALLE (jabas/peso por viaje, por variedad, dentro de un corte)
-- ============================================
CREATE TABLE corte_variedad_detalle (
    id              BIGSERIAL PRIMARY KEY,
    corte_id        BIGINT NOT NULL REFERENCES corte(id) ON DELETE CASCADE,
    variedad        VARCHAR(80) NOT NULL,
    jabas           NUMERIC(10,0) NOT NULL DEFAULT 0 CHECK (jabas >= 0),
    peso_por_viaje  NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (peso_por_viaje >= 0),
    orden           SMALLINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cvd_corte ON corte_variedad_detalle(corte_id);

-- ============================================
-- CORTE_MAQUINA_KATO (detalle operativo: maquina, kato, empacadores, kg/emp)
-- ============================================
CREATE TABLE corte_maquina_kato (
    id                  BIGSERIAL PRIMARY KEY,
    corte_id            BIGINT NOT NULL REFERENCES corte(id) ON DELETE CASCADE,
    maquina_id          BIGINT NOT NULL REFERENCES maquina(id),
    kato_nombre         VARCHAR(30) NOT NULL,
    empacadores         SMALLINT NOT NULL DEFAULT 0 CHECK (empacadores >= 0),
    kg_por_empacador    NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (kg_por_empacador >= 0),
    orden               SMALLINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cmk_corte ON corte_maquina_kato(corte_id);
CREATE INDEX idx_cmk_maquina ON corte_maquina_kato(maquina_id);

-- ============================================
-- TRIGGER generico para actualizar updated_at automaticamente
-- (segunda capa de defensa ademas de @LastModifiedDate de JPA,
-- cubre updates que ocurran fuera de la aplicacion)
-- ============================================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuario_updated_at BEFORE UPDATE ON usuario
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_producto_updated_at BEFORE UPDATE ON producto
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_campana_updated_at BEFORE UPDATE ON campana
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_maquina_updated_at BEFORE UPDATE ON maquina
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_proceso_updated_at BEFORE UPDATE ON proceso_diario
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_mp_plan_updated_at BEFORE UPDATE ON materia_prima_plan
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_corte_updated_at BEFORE UPDATE ON corte
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_cvd_updated_at BEFORE UPDATE ON corte_variedad_detalle
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_cmk_updated_at BEFORE UPDATE ON corte_maquina_kato
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
