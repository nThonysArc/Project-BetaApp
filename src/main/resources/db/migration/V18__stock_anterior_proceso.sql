-- Stock que quedo del dia anterior (lo digita el supervisor junto con la materia prima). NULL = sin dato manual.
ALTER TABLE proceso_diario ADD COLUMN stock_anterior_jabas NUMERIC(10, 0);
ALTER TABLE proceso_diario ADD COLUMN stock_anterior_fecha DATE;
