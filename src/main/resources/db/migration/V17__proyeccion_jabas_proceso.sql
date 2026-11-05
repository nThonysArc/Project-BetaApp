-- Proyeccion de jabas del dia (dato propio de la plantilla Vid; la digita el asistente junto con la materia prima).
-- NULL = no se ingreso: el reporte usa entonces el total ingresado, como antes.
ALTER TABLE proceso_diario ADD COLUMN proyeccion_jabas NUMERIC(10, 0);
