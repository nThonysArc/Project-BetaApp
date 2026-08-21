-- ============================================================
-- V7: Correccion de mapeo enum <-> Hibernate
--
-- usuario.rol y proceso_diario.estado se crearon en V1 como tipos ENUM
-- nativos de PostgreSQL (rol_usuario, estado_proceso). Las entidades JPA
-- usan @Enumerated(EnumType.STRING), que Hibernate mapea por defecto a
-- VARCHAR, no al tipo OTHER que representa un enum nativo de Postgres.
--
-- hibernate.ddl-auto=validate no atrapa este desalineamiento al arrancar,
-- pero cualquier INSERT/UPDATE real sobre esas columnas falla en runtime
-- porque Postgres rechaza la conversion implicita de varchar a su tipo
-- enum nativo (SQLState 42804).
--
-- Se estandariza a VARCHAR + CHECK, igual patron que ya usamos en
-- corte.estado, evitando la complejidad de mapear enums nativos de
-- Postgres en JPA.
-- ============================================================

-- ---- usuario.rol ----
ALTER TABLE usuario
    ALTER COLUMN rol TYPE VARCHAR(20) USING rol::text;

ALTER TABLE usuario
    ADD CONSTRAINT chk_usuario_rol CHECK (rol IN ('ADMIN', 'SUPERVISOR', 'OPERADOR'));

-- ---- proceso_diario.estado ----
ALTER TABLE proceso_diario
    ALTER COLUMN estado DROP DEFAULT;

ALTER TABLE proceso_diario
    ALTER COLUMN estado TYPE VARCHAR(20) USING estado::text;

ALTER TABLE proceso_diario
    ALTER COLUMN estado SET DEFAULT 'ABIERTO';

ALTER TABLE proceso_diario
    ADD CONSTRAINT chk_proceso_diario_estado CHECK (estado IN ('ABIERTO', 'CERRADO'));

-- ---- limpieza: los tipos enum nativos ya no se usan ----
DROP TYPE IF EXISTS rol_usuario;
DROP TYPE IF EXISTS estado_proceso;
