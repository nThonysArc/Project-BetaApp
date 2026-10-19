-- ============================================================
-- V14: Permisos por usuario (checklist) y rol SUPER_ADMIN
-- ============================================================

-- 1) Nuevo rol permitido
ALTER TABLE usuario DROP CONSTRAINT IF EXISTS chk_usuario_rol;
ALTER TABLE usuario ADD CONSTRAINT chk_usuario_rol
    CHECK (rol IN ('SUPER_ADMIN', 'ADMIN', 'SUPERVISOR', 'OPERADOR'));

-- 2) Permisos concedidos a cada usuario (el catalogo vive en el codigo: enum Permiso)
CREATE TABLE usuario_permiso (
    usuario_id BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    permiso    VARCHAR(60) NOT NULL,
    PRIMARY KEY (usuario_id, permiso)
);

-- 3) Backfill: nadie pierde lo que hoy puede hacer.
--    ADMIN: todo.
INSERT INTO usuario_permiso (usuario_id, permiso)
SELECT u.id, p.permiso
FROM usuario u
CROSS JOIN (VALUES
    ('PROCESOS_VER'), ('PROCESOS_CREAR'), ('PROCESOS_CERRAR'), ('PROCESOS_REABRIR'),
    ('MATERIA_PRIMA_VER'), ('MATERIA_PRIMA_EDITAR'),
    ('CORTES_VER'), ('CORTES_CREAR'), ('CORTES_EDITAR'), ('CORTES_EDITAR_CERRADO'), ('CORTES_AJUSTAR_TOTALES'),
    ('REPORTE_VER'), ('CATALOGOS_ADMINISTRAR'), ('SUPERVISORES_ADMINISTRAR')
) AS p(permiso)
WHERE u.rol = 'ADMIN';

--    SUPERVISOR / OPERADOR: toda la operacion diaria (como hasta ahora), sin administracion de catalogos.
INSERT INTO usuario_permiso (usuario_id, permiso)
SELECT u.id, p.permiso
FROM usuario u
CROSS JOIN (VALUES
    ('PROCESOS_VER'), ('PROCESOS_CREAR'), ('PROCESOS_CERRAR'), ('PROCESOS_REABRIR'),
    ('MATERIA_PRIMA_VER'), ('MATERIA_PRIMA_EDITAR'),
    ('CORTES_VER'), ('CORTES_CREAR'), ('CORTES_EDITAR'), ('CORTES_EDITAR_CERRADO'), ('CORTES_AJUSTAR_TOTALES'),
    ('REPORTE_VER')
) AS p(permiso)
WHERE u.rol IN ('SUPERVISOR', 'OPERADOR');

-- 4) Super admin inicial.
--    Contrasena TEMPORAL: ver LEEME del parche. Cambiala al primer ingreso (Usuarios > editar).
INSERT INTO usuario (nombre_completo, email, password_hash, rol, activo)
VALUES (
    'Super Administrador',
    'superadmin@betaagroindustrial.pe',
    '$2a$10$rDEirXSo.8mb4/3.c3NBBeQImkw.FcX7oBRbo84y8sDkSTXzHjwai',
    'SUPER_ADMIN',
    TRUE
);
