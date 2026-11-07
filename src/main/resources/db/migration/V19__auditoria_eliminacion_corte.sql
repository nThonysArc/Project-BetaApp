-- ============================================================
-- V19: la auditoria admite el tipo de cambio ELIMINACION (borrado de un corte).
-- El permiso CORTES_ELIMINAR no necesita migracion: el catalogo vive en el codigo (enum Permiso) y
-- usuario_permiso solo guarda lo concedido. Nadie lo tiene hasta que el super admin lo marque.
-- ============================================================
ALTER TABLE auditoria_edicion DROP CONSTRAINT IF EXISTS chk_auditoria_tipo_cambio;
ALTER TABLE auditoria_edicion ADD CONSTRAINT chk_auditoria_tipo_cambio
    CHECK (tipo_cambio IN ('EDICION_INPUT', 'AJUSTE_MANUAL', 'RECALCULO', 'ELIMINACION'));
