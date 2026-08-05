-- ============================================================
-- V6: Completar columnas de auditoria para la tabla usuario
-- La entidad Usuario tambien hereda de Auditable, por lo que 
-- requiere rastrear que usuario del sistema lo creo/modifico.
-- ============================================================

ALTER TABLE usuario 
    ADD COLUMN created_by BIGINT REFERENCES usuario(id),
    ADD COLUMN updated_by BIGINT REFERENCES usuario(id);