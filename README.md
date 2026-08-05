# Avance de Producción — Beta Agroindustrial

Sistema de registro de avance de producción por hora. Reemplaza el flujo manual
(papel → Excel → captura de pantalla → correo/WhatsApp) por una app web/móvil
con formulario simplificado, cálculo automático de acumulados, generación de
imagen del reporte y funcionamiento **offline-first** (crítico: la señal se
pierde justo en la zona de planta donde se captura la data).

**MVP Fase 1**: alcance inicial arándano. Arquitectura multi-producto desde
el diseño (esquema de BD y paquetes), lista para espárrago/uva/granada sin
refactor de estructura.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21 · Spring Boot 3.3 · Spring Security (JWT) · Spring Data JPA |
| Base de datos | PostgreSQL 16 · Flyway |
| Mapeo | MapStruct · Lombok |
| Exportación | Apache POI (Excel) |
| Docs API | SpringDoc OpenAPI (Swagger UI) |
| Frontend (repo separado) | Angular 18 (PWA) · Capacitor (empaquetado móvil futuro) |
| Despliegue | Railway (backend + Postgres) · Vercel (frontend) |

## Estructura del proyecto

```
src/main/java/pe/betaagroindustrial/avance/
├── config/              → SecurityConfig, CorsConfig, OpenApiConfig, AuditorAwareConfig
├── common/              → GlobalExceptionHandler, excepciones de dominio, clase Auditable
├── security/            → JWT, AuthController/Service, UserDetailsServiceImpl
├── producto/            → catálogo multi-producto (CRUD de referencia)
├── campana/             → campañas por producto
├── maquina/             → máquinas y supervisores por campaña
├── proceso/             → dominio central
│   ├── materiaprima/    → plan diario por variedad ("TOTAL A PROCESAR")
│   ├── corte/           → registro por hora (entidad central del sistema)
│   └── auditoria/       → event log de ediciones
├── usuario/              → usuarios y roles (ADMIN/SUPERVISOR/OPERADOR)
├── reporte/              → generación de imagen y exportación Excel (pendiente)
└── mapper/               → interfaces MapStruct Entity↔DTO

src/main/resources/
├── application.yml           → configuración base
├── application-dev.yml       → perfil desarrollo local
├── application-prod.yml      → perfil producción (Railway)
└── db/migration/             → migraciones Flyway versionadas
```

## Decisiones de arquitectura clave

- **Single-schema multi-producto**: una sola base de datos, con `producto`
  como tabla discriminadora. Agregar un cultivo nuevo es un `INSERT`, no un
  despliegue de infraestructura.
- **Calculado vs. ajustado**: los totales (`jabas_total_calculado`,
  `peso_total_calculado`) nunca se sobrescriben. Un ajuste manual se guarda
  en una columna paralela (`*_ajustado`); el valor efectivo es siempre
  `COALESCE(ajustado, calculado)`. Ver `Corte.getJabasTotalEfectivo()`.
- **Auditoría genérica**: `auditoria_edicion` es un event log único para
  cualquier entidad del dominio de proceso — evita duplicar tablas de
  historial por cada entidad.
- **Idempotencia offline-first**: cada `Corte`, `CorteVariedadDetalle` y
  `CorteMaquinaKato` tiene un `cliente_uuid` único, generado en el
  dispositivo antes de sincronizar. Si el POST se reintenta tras una
  pérdida de señal a mitad del envío, el backend no duplica el registro.
- **Seguridad stateless (JWT)**: sin sesiones de servidor, para que cada
  sincronización desde el dispositivo sea una petición autocontenida.

## Requisitos previos

- Java 21 (JDK)
- Maven 3.9+ (o usar el `Dockerfile`, que no requiere Maven instalado localmente)
- Docker + Docker Compose (recomendado para desarrollo local)

## Arranque local con Docker Compose (recomendado)

```bash
cp .env.example .env
# editar .env si es necesario

docker compose up --build
```

Esto levanta PostgreSQL + backend. Flyway corre las migraciones automáticamente
al iniciar. La API queda disponible en `http://localhost:8080`.

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Arranque local sin Docker

```bash
# 1. Levantar solo Postgres
docker compose up postgres -d

# 2. Correr el backend con Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Usuario inicial (seed)

La migración `V3__seed_producto_arandano.sql` crea:

- **Email**: `admin@betaagroindustrial.pe`
- **Password**: `ChangeMe123!`
- **Rol**: `ADMIN`

**Cambiar esta contraseña inmediatamente** después del primer despliegue
(vía endpoint de cambio de contraseña — pendiente de implementar, o
directamente en BD generando un nuevo hash BCrypt).

## Probar el login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@betaagroindustrial.pe","password":"ChangeMe123!"}'
```

Devuelve un JWT que se debe enviar en las siguientes peticiones como
`Authorization: Bearer <token>`.

## Migraciones (Flyway)

Nunca usar `hibernate.ddl-auto=update`. Todo cambio de esquema es un
archivo nuevo en `src/main/resources/db/migration/`, con el patrón
`V{n}__descripcion.sql`. Los archivos ya aplicados **nunca se editan**.

| Migración | Contenido |
|---|---|
| V1 | Esquema base: producto, campaña, máquina, proceso_diario, materia_prima_plan, corte, corte_variedad_detalle, corte_maquina_kato, usuario |
| V2 | Ajustes manuales (calculado/ajustado), consolidación de cortes, auditoría genérica, `cliente_uuid` para idempotencia offline |
| V3 | Seed: producto Arándano + usuario admin inicial |

## Despliegue en Railway

1. **Provisionar PostgreSQL**: New Project → Add Plugin → PostgreSQL.
2. **Backend**: New Service → Deploy from GitHub repo (este repositorio).
   Railway detecta el `Dockerfile` automáticamente.
3. **Variables de entorno** del servicio backend (Settings → Variables):

   ```
   SPRING_PROFILES_ACTIVE=prod
   SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
   SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
   SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
   JWT_SECRET=<generar con: openssl rand -base64 32>
   JWT_EXPIRATION_MS=86400000
   CORS_ALLOWED_ORIGINS=https://<dominio-del-frontend-en-vercel>
   ```

4. Railway asigna el `PORT` automáticamente (ya leído vía `${PORT:8080}`
   en `application.yml`).
5. **Frontend**: desplegar por separado en Vercel/Netlify, apuntando
   `environment.apiUrl` al dominio del backend en Railway.

## Próximos pasos (fuera de este MVP inicial)

- `CorteService`: lógica de "clonar corte anterior" (plantilla de siguiente
  hora con estructura replicada, horas auto-calculadas).
- Endpoints de `ProcesoDiario`, `Campana`, `Maquina`, `Corte` completos
  (este proyecto solo incluye `Producto` como CRUD de referencia).
- `ReporteImagenService`: generación de imagen del reporte (HTML → PNG
  vía Playwright headless, o Java2D).
- `ReporteExcelService`: exportación con Apache POI.
- Endpoint de resumen/KPIs con totales y acumulados calculados.
- Frontend Angular con PWA + IndexedDB (Dexie.js) para la cola de
  sincronización offline-first (outbox pattern).
