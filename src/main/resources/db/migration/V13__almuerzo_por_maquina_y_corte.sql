-- Una maquina puede salir a almorzar en una hora determinada (sus filas quedan en 0).
-- Si TODAS las maquinas del corte estan en almuerzo, el reporte dibuja la barra "ALMUERZO DEL PERSONAL".
ALTER TABLE corte_maquina ADD COLUMN almuerzo BOOLEAN NOT NULL DEFAULT FALSE;
