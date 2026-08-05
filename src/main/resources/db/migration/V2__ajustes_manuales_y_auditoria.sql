-- ============================================================
-- V2: Ajustes manuales sobre valores calculados, consolidacion
-- de cortes, auditoria de ediciones, e idempotencia para
-- sincronizacion offline-first.
-- ============================================================

-- ============================================
-- Ajustes a nivel de CORTE (total por hora)
-- Regla: valor_efectivo = COALESCE(valor_ajustado, valor_calculado)
-- El calculado NUNCA se sobrescribe; el ajuste vive al lado.
-- ============================================
ALTER TABLE corte
    ADD COLUMN jabas_total_calculado    NUMERIC(10,0),
    ADD COLUMN jabas_total_ajustado     NUMERIC(10,0) CHECK (jabas_total_ajustado IS NULL OR jabas_total_ajustado >= 0),
    ADD COLUMN peso_total_calculado     NUMERIC(12,2),
    ADD COLUMN peso_total_ajustado      NUMERIC(12,2) CHECK (peso_total_ajustado IS NULL OR peso_total_ajustado >= 0),
    ADD COLUMN estado                   VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    ADD COLUMN consolidado_en           TIMESTAMPTZ,
    ADD COLUMN consolidado_por          BIGINT REFERENCES usuario(id),
    ADD COLUMN requiere_revision        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN cliente_uuid             UUID UNIQUE,
    ADD CONSTRAINT chk_corte_estado CHECK (estado IN ('BORRADOR', 'CONSOLIDADO'));

CREATE INDEX idx_corte_estado ON corte(estado);
CREATE INDEX idx_corte_cliente_uuid ON corte(cliente_uuid);

COMMENT ON COLUMN corte.cliente_uuid IS
    'UUID generado en el dispositivo al crear el registro offline. '
    'Usado como clave de idempotencia al sincronizar: si ya existe, '
    'el backend no duplica el corte aunque el cliente reintente el envio.';

COMMENT ON COLUMN corte.requiere_revision IS
    'Se activa automaticamente cuando se edita un corte anterior que '
    'afecta el calculo base de un ajuste manual posterior aguas abajo. '
    'No se recalcula/borra el ajuste automaticamente: se marca para '
    'que un usuario decida si sigue vigente o debe actualizarse.';

-- ============================================
-- Ajustes a nivel de PROCESO_DIARIO (acumulado, stock)
-- Tabla de eventos append-only: el ajuste vigente es el mas reciente
-- por proceso_diario_id (ORDER BY ajustado_en DESC).
-- ============================================
CREATE TABLE proceso_diario_ajuste (
    id                          BIGSERIAL PRIMARY KEY,
    proceso_diario_id           BIGINT NOT NULL REFERENCES proceso_diario(id) ON DELETE CASCADE,
    jabas_procesado_ajustado    NUMERIC(10,0),
    kilos_procesado_ajustado    NUMERIC(12,2),
    stock_jabas_ajustado        NUMERIC(10,0),
    stock_kilos_ajustado        NUMERIC(12,2),
    motivo                      TEXT,
    ajustado_por                BIGINT NOT NULL REFERENCES usuario(id),
    ajustado_en                 TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pda_proceso ON proceso_diario_ajuste(proceso_diario_id, ajustado_en DESC);

-- ============================================
-- HISTORIAL DE AUDITORIA GENERAL (event log de cualquier edicion)
-- ============================================
CREATE TABLE auditoria_edicion (
    id              BIGSERIAL PRIMARY KEY,
    entidad         VARCHAR(50) NOT NULL,
    entidad_id      BIGINT NOT NULL,
    campo           VARCHAR(60) NOT NULL,
    valor_anterior  TEXT,
    valor_nuevo     TEXT,
    tipo_cambio     VARCHAR(20) NOT NULL,
    motivo          TEXT,
    usuario_id      BIGINT NOT NULL REFERENCES usuario(id),
    fecha           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_auditoria_tipo_cambio CHECK (tipo_cambio IN ('EDICION_INPUT', 'AJUSTE_MANUAL', 'RECALCULO'))
);

CREATE INDEX idx_auditoria_entidad ON auditoria_edicion(entidad, entidad_id, fecha DESC);
CREATE INDEX idx_auditoria_usuario ON auditoria_edicion(usuario_id, fecha DESC);

-- ============================================
-- Idempotencia de sincronizacion offline-first tambien para
-- detalle de variedad y maquina/kato (mismos registros que puede
-- reintentar el dispositivo tras perdida de senal a mitad del envio)
-- ============================================
ALTER TABLE corte_variedad_detalle
    ADD COLUMN cliente_uuid UUID UNIQUE;

ALTER TABLE corte_maquina_kato
    ADD COLUMN cliente_uuid UUID UNIQUE;
