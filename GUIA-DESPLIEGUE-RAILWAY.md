# Despliegue en Railway (BetaApp)

Tres servicios en un mismo proyecto de Railway: **Postgres**, **backend** y **frontend**.
Ambos repos se despliegan desde la rama `main` de GitHub.

## Orden
1. New Project > Provision PostgreSQL.
2. New > GitHub Repo > `Project-BetaApp` (backend). Railway detecta el Dockerfile.
3. Variables del backend (Settings > Variables):
   | Variable | Valor |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
   | `SPRING_DATASOURCE_USERNAME` | `${{Postgres.PGUSER}}` |
   | `SPRING_DATASOURCE_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
   | `JWT_SECRET` | aleatorio, minimo 32 caracteres (`openssl rand -base64 48`) |
   | `CORS_ALLOWED_ORIGINS` | URL exacta del frontend, sin `/` final (se completa en el paso 6) |
   Luego Settings > Networking > Generate Domain (anota la URL del backend).
4. New > GitHub Repo > `BETAPP---FRONTEND-ANGULAR`.
5. Variable del frontend: `API_URL` = `https://<dominio-backend>.up.railway.app/api`
   Generate Domain (anota la URL del frontend).
6. Vuelve al backend y completa `CORS_ALLOWED_ORIGINS` con la URL del frontend (redespliega solo).

## Primer ingreso (OBLIGATORIO)
Las migraciones crean usuarios iniciales con contrasenas conocidas (el repo es publico):
- `admin@betaagroindustrial.pe` (contrasena inicial en V3)
- `superadmin@betaagroindustrial.pe` (contrasena temporal del LEEME del parche)
Entra con el super admin, cambia ambas contrasenas desde Usuarios > editar, y desactiva lo que no uses.

## Verificar
- Backend: `https://<backend>/actuator/health` responde `{"status":"UP"}`.
- Frontend: abre la URL, inicia sesion, genera un reporte PNG de prueba.

## Si algo falla
- Backend no arranca y el log menciona `JWT_SECRET` o `CORS_ALLOWED_ORIGINS`: falta la variable.
- Frontend: el build falla con "falta la variable API_URL": defínela y redespliega.
- Login falla con error CORS: `CORS_ALLOWED_ORIGINS` no coincide exacto con la URL del frontend.
- Cambiar `API_URL` exige redesplegar el frontend (es constante de compilacion).
