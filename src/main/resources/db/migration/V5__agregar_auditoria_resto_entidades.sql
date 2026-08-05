-- ============================================================
-- V5: Completar columnas de auditoria en el esquema
-- Sincronizacion de dependencias created_by y updated_by para 
-- todas las entidades del dominio que heredan de Auditable.
-- ============================================================

ALTER TABLE producto 
    ADD COLUMN created_by BIGINT REFERENCES usuario(id),
    ADD COLUMN updated_by BIGINT REFERENCES usuario(id);

ALTER TABLE maquina 
    ADD COLUMN created_by BIGINT REFERENCES usuario(id),
    ADD COLUMN updated_by BIGINT REFERENCES usuario(id);

ALTER TABLE proceso_diario 
    ADD COLUMN created_by BIGINT REFERENCES usuario(id),
    ADD COLUMN updated_by BIGINT REFERENCES usuario(id);

ALTER TABLE materia_prima_plan 
    ADD COLUMN created_by BIGINT REFERENCES usuario(id),
    ADD COLUMN updated_by BIGINT REFERENCES usuario(id);

ALTER TABLE corte_variedad_detalle 
    ADD COLUMN created_by BIGINT REFERENCES usuario(id),
    ADD COLUMN updated_by BIGINT REFERENCES usuario(id);

ALTER TABLE corte_maquina_kato 
    ADD COLUMN created_by BIGINT REFERENCES usuario(id),
    ADD COLUMN updated_by BIGINT REFERENCES usuario(id);