-- ============================================================
-- V10: El cierre pasa a ser por ProcesoDiario, no por Corte individual
--
-- El concepto de "corte consolidado" (bloquear una hora especifica y
-- exigir motivo para editarla) no encaja con el flujo real: hasta la
-- ultima hora del dia se puede necesitar ajustar el peso de la primera
-- hora, para que cuadre con el ingreso total reportado. El punto de
-- cierre real es el PROCESO DIARIO completo (ya existia:
-- proceso_diario.estado ABIERTO/CERRADO), no cada corte por separado.
-- ============================================================

ALTER TABLE corte DROP COLUMN estado;
ALTER TABLE corte DROP COLUMN consolidado_en;
ALTER TABLE corte DROP COLUMN consolidado_por;
