-- ============================================================
-- V12: Observacion por MAQUINA y por CORTE (hora)
--
-- En el reporte real de avance cada maquina tiene su propia
-- observacion en cada hora (ej: "paro por espera de MP", "falla de
-- caida de bandeja"), no una sola observacion por corte. Se guarda en
-- corte_maquina, que ya existe por (corte, maquina) desde V11.
--
-- corte.observacion se conserva como observacion general del corte.
-- ============================================================

ALTER TABLE corte_maquina ADD COLUMN observacion TEXT;
