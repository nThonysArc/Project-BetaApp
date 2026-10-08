# Parche backend: supervisor por corte (hora) y maquina

Extrae este ZIP SOBRE LA RAIZ del repo `Project-BetaApp` (acepta reemplazar) y haz commit.
Se verifico solo la sintaxis del Java; NO se pudo compilar ni probar contra PostgreSQL en el entorno donde se genero (sin acceso a Maven Central). Compila local antes de subir:

    mvnw.cmd -q -DskipTests compile

## Que cambia
- `V11__supervisor_por_corte_y_maquina.sql`: nueva tabla `corte_maquina` (corte + maquina -> supervisor), UNICA por (corte_id, maquina_id) = un solo supervisor por maquina y por hora. Rellena los cortes existentes con el supervisor que tenia asignada cada maquina. `maquina.supervisor_id` se conserva como valor SUGERIDO.
- `CorteMaquina` (entidad nueva) y `Corte.supervisores`.
- `CorteRequest`: campo opcional `supervisores` [{maquinaId, supervisorId}]. `CorteResponse` y `CortePlantillaResponse` devuelven `supervisores`.
- `CorteService`: por cada maquina del detalle el supervisor se toma, en este orden: lo enviado -> el que ya tenia el corte (al editar) -> el del corte anterior -> el sugerido de la maquina. Duplicados o maquinas fuera del detalle -> 422.
- `GET /api/usuarios?rol=SUPERVISOR` (usuarios activos; solo id, nombre y rol).
- `ReporteImagenService`: columna "Supervisor" en el detalle por hora; el resumen por maquina muestra un nombre, o los tramos con horario si hubo rotacion.

## Compatibilidad
Cambios aditivos: el frontend anterior sigue funcionando (si no envia `supervisores`, se heredan). Orden de despliegue: BACKEND primero, luego frontend.

## Pruebas (Windows CMD; reemplaza TOKEN, URL, ids)
    curl -s "https://URL/api/usuarios?rol=SUPERVISOR" -H "Authorization: Bearer TOKEN"

    curl -s "https://URL/api/procesos/5/cortes/plantilla-siguiente" -H "Authorization: Bearer TOKEN"

    curl -s -X POST "https://URL/api/procesos/5/cortes" ^
      -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" ^
      -d "{\"clienteUuid\":\"11111111-1111-1111-1111-111111111111\",\"horaInicio\":\"10:00\",\"horaFin\":\"11:00\",\"detalles\":[{\"maquinaId\":1,\"maquinaKatoId\":1,\"variedadId\":1,\"jabas\":10,\"pesoTotal\":100,\"empacadores\":3,\"kgPorEmpacador\":33.33,\"orden\":1}],\"supervisores\":[{\"maquinaId\":1,\"supervisorId\":2}]}"

La respuesta debe incluir `"supervisores":[{"maquinaId":1,...,"supervisorId":2,...}]`. Luego descarga `/api/procesos/5/reporte-imagen` y revisa la columna Supervisor.
