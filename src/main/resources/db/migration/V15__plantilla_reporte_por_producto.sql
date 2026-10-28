-- V15: Plantilla de reporte por producto + detalle de corte sin kato obligatorio.
--
-- Cada producto elige el formato de su reporte de avance por hora:
--   ARANDANO -> maquina + kato (kilos, kg por empacador)   [comportamiento actual]
--   VID      -> "Avance por hora de seleccion": modulo, personas, jabas, rend/persona
-- Todos los productos existentes siguen con ARANDANO: la migracion no cambia lo que ya funciona.
ALTER TABLE producto
    ADD COLUMN plantilla_reporte VARCHAR(20) NOT NULL DEFAULT 'ARANDANO';

ALTER TABLE producto
    ADD CONSTRAINT chk_producto_plantilla_reporte CHECK (plantilla_reporte IN ('ARANDANO', 'VID'));

-- Si ya existe el producto Vid/Uva, se le asigna su plantilla (si no existe, se hace desde Productos).
UPDATE producto SET plantilla_reporte = 'VID' WHERE lower(nombre) IN ('vid', 'vides', 'uva', 'uvas');

-- Vid no trabaja con katos: la fila de detalle es modulo (maquina) + variedad.
ALTER TABLE corte_detalle ALTER COLUMN maquina_kato_id DROP NOT NULL;
