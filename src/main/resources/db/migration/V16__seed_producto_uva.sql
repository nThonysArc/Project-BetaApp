-- ============================================================
-- V16: Producto Uva (Vid) con su plantilla de reporte.
--
-- Por que una migracion nueva y no editar V3: Flyway guarda el checksum de cada migracion
-- ya aplicada. Si se modifica V3, todas las bases existentes (local y Railway) fallan
-- al validar.
--
-- Es idempotente: si el producto ya existe (creado a mano como "Vid", "Uva", etc.)
-- no se duplica; solo se asegura que use la plantilla VID y quede activo.
-- ============================================================
INSERT INTO producto (nombre, unidad_medida, activo, plantilla_reporte)
SELECT 'Uva', 'KG', TRUE, 'VID'
WHERE NOT EXISTS (
    SELECT 1 FROM producto WHERE lower(nombre) IN ('vid', 'vides', 'uva', 'uvas')
);

UPDATE producto
   SET plantilla_reporte = 'VID',
       activo = TRUE
 WHERE lower(nombre) IN ('vid', 'vides', 'uva', 'uvas');
