-- ============================================================
-- V3: Datos iniciales minimos para arrancar el MVP Fase 1 (arandano)
-- ============================================================

-- Producto inicial en alcance. Espararrago/uva/granada se agregan luego
-- con un simple INSERT, sin cambios de esquema.
INSERT INTO producto (nombre, unidad_medida, activo)
VALUES ('Arandano', 'KG', TRUE);

-- Usuario administrador inicial.
-- Password hash corresponde a "ChangeMe123!" con BCrypt - DEBE cambiarse
-- inmediatamente despues del primer despliegue.
INSERT INTO usuario (nombre_completo, email, password_hash, rol, activo)
VALUES (
    'Administrador Sistema',
    'admin@betaagroindustrial.pe',
    '$2b$10$Ib7hoiTCN8v3Jd2RfvjWMOzZSjQpBx.7gFxxQ7pUb7BgQHJGGg38i',
    'ADMIN',
    TRUE
);
