-- ============================================================
-- V4: Sincronizacion del modelo relacional con la superclase
-- Auditable de Java. Se añaden las columnas de rastreo de 
-- usuarios a las entidades del dominio.
-- ============================================================

ALTER TABLE campana 
    ADD COLUMN created_by BIGINT REFERENCES usuario(id),
    ADD COLUMN updated_by BIGINT REFERENCES usuario(id);

-- Si Producto, Maquina y ProcesoDiario tambien heredan de Auditable en Java,
-- descomenta las siguientes lineas para evitar futuros errores de validacion:

-- ALTER TABLE producto 
--     ADD COLUMN created_by BIGINT REFERENCES usuario(id),
--     ADD COLUMN updated_by BIGINT REFERENCES usuario(id);

-- ALTER TABLE maquina 
--     ADD COLUMN created_by BIGINT REFERENCES usuario(id),
--     ADD COLUMN updated_by BIGINT REFERENCES usuario(id);

-- ALTER TABLE proceso_diario 
--     ADD COLUMN created_by BIGINT REFERENCES usuario(id),
--     ADD COLUMN updated_by BIGINT REFERENCES usuario(id);