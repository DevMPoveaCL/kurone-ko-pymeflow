# Exploration: Cockpit Demo Seed/Reset MVP

## Current State

El sistema PymeFlow carece de cualquier mecanismo de carga o reseteo de datos demo. Los datos se ingresan exclusivamente mediante acciones manuales del cockpit (`POST /api/cashflow/imports/manual` con 4 filas sample) o sync de proveedores fixture (`POST /api/cashflow/provider-syncs` con `santander` o `bancoestado`). No existe endpoint, comando o migration que cargue un dataset demo predefinido ni que permita limpiar movimientos acumulados para repetir una demo.

### Puntos de entrada de datos (sin cambios)

| Endpoint | Método | Rol en MVP |
|---|---|---|
| `/api/cashflow/imports/manual` | POST | Importación manual con tolerancia por fila. El cockpit envía 4 filas hardcodeadas en `app.js` (`SAMPLE_ROWS`). |
| `/api/cashflow/provider-syncs` | POST | Sync fixture desde `santander-page-1.json` o `bancoestado-page-1.json`. |
| `/api/cashflow/cockpit/preferences` | PUT | Persiste preferencias de cockpit (opening balance, horizon). |

### Tablas existentes y su naturaleza

| Tabla | Tipo de dato | ¿Reseteable? |
|---|---|---|
| `vertical_profiles` | Referencia (seeded por migration) | **NO** — es la definición del perfil |
| `vertical_profile_categories` | Referencia (seeded por `R__seed_pharmacy_categories.sql`) | **NO** — categorías del perfil |
| `vertical_profile_rules` | Referencia (seeded) | **NO** — reglas de proyección |
| `vertical_profile_obligation_templates` | Referencia (seeded) | **NO** — plantillas de obligaciones |
| `cashflow_movement_history` | **Transaccional** — movimientos ingeridos | **SÍ** — scope demo |
| `provider_sync_sessions` | **Transaccional** — sesiones de sync | **SÍ** — scope demo |
| `cockpit_preferences` | **Transaccional** — preferencias del usuario | **SÍ** — scope demo (discutible) |

### Mecanismo de seeding existente

El único mecanismo de seeding es la migration Flyway repeatable `R__seed_pharmacy_categories.sql`, que usa `INSERT ... ON CONFLICT DO UPDATE` para asegurar idempotencia. Este patrón es adecuado para datos de REFERENCIA (categorías, reglas, obligaciones), pero NO para datos transaccionales (movimientos) porque:

1. No tiene contraparte de limpieza.
2. Los movimientos usan UUIDs generados — cada ejecución inserta nuevos.
3. Las sesiones de sync son mutables (cursor, last_sync_at) — no aptas para `ON CONFLICT DO UPDATE` sin perder historia.

### Cómo funciona hoy la demo manual

El cockpit (`app.js`) hace manualmente:
1. `POST /api/cashflow/imports/manual` con 4 filas `SAMPLE_ROWS` (2 CREDIT, 1 DEBIT, 1 inválida)
2. `POST /api/cashflow/provider-syncs` con `santander` (carga 2 entradas desde fixture JSON)
3. Luego el usuario categoriza movimientos en revisión, ingresa saldo inicial y ejecuta proyección

Para repetir la demo hoy: **borrar la base de datos y reiniciar la app** — Flyway recrea las tablas y el seed de referencia. Esto es inviable para una demo repetible en vivo.

### Arquitectura hexagonal — restricciones

```
domain/          → SIN imports de framework, SIN literales de banco/proveedor
application/     → Define puertos (interfaces puras), SIN imports de infraestructura
infrastructure/  → Implementa puertos, usa JdbcTemplate/JPA
interfaces/      → Controladores REST, DTOs
```

- **ArchUnit** prohíbe literales como `banco`, `bank`, `acquirer` en `domain/` y `application/`.
- Puerto existente relevante: `CashflowMovementHistoryPort` — expone `saveAll`, `findByStatus`, `resolveManualReview`, etc. **No expone `delete` ni `truncate`**.
- `SyncSessionPort` — expone `syncId`, `findCursor`, `saveCursor`, `recordReport`. **No expone `delete`**.

## Affected Areas

| Área | Archivo(s) | Impacto |
|---|---|---|
| **Nuevo puerto** | `application/port/out/DemoDataPort.java` | Puerto para operaciones demo: `reset` + `seed`. Usa nombres agnósticos (no "bank", "banco"). |
| **Nueva implementación** | `infrastructure/demo/JdbcDemoDataAdapter.java` | Implementación JDBC del puerto. Contiene SQL de DELETE + INSERT para movimientos y sesiones demo. |
| **Nuevo endpoint (opcional, ver approaches)** | `interfaces/web/DemoController.java` | Endpoint REST `POST /api/cockpit/demo/reset` y/o `POST /api/cockpit/demo/seed`. |
| **Nueva migration** | `src/main/resources/db/migration/V7__add_demo_column.sql` | (Solo para Approach 2) Agrega columna `is_demo` a tablas transaccionales. |
| **Cockpit HTML/JS** | `src/main/resources/static/index.html`, `app.js` | Agrega botón(es) "Cargar datos demo" y "Reiniciar demo" en el cockpit. |
| **ArchUnit** | `src/test/.../ArchitectureTest.java` | Posible ajuste si el paquete `infrastructure/demo/` usa literales restringidos (debe no hacerlo). |

## Approaches

### 1. Reset + Seed explícito vía `DemoDataPort` (RECOMENDADO)

Un nuevo puerto `DemoDataPort` en `application/port/out/` con dos operaciones:
- `seed(ProfileId)` — carga un dataset demo fijo en `cashflow_movement_history` y `provider_sync_sessions`
- `reset(ProfileId)` — borra (DELETE) todos los movimientos y sesiones de sync del perfil activo

La implementación `JdbcDemoDataAdapter` en `infrastructure/demo/` ejecuta SQL directo con `JdbcTemplate`. El dataset demo SEED se define como listas Java inline (mismos datos que `SAMPLE_ROWS` de `app.js` + entradas de fixture JSON), sin archivos externos que compliquen el deployment.

Se expone opcionalmente vía un controller `POST /api/cockpit/demo/reset` y `POST /api/cockpit/demo/seed`, o se combinan en un solo endpoint `POST /api/cockpit/demo/reset-and-seed`.

**Flujo demo completo**:
```
1. Usuario clickea "Reiniciar demo"
2. Cockpit llama POST /api/cockpit/demo/reset-and-seed
3. Backend:
   a. DELETE FROM cashflow_movement_history WHERE profile_id = ?
   b. DELETE FROM provider_sync_sessions WHERE profile_id = ?
   c. DELETE FROM cockpit_preferences WHERE profile_id = ?  (decidible)
   d. INSERT movimientos demo (4+ filas con direcciones DEBIT/CREDIT)
   e. INSERT sesiones de sync demo (snapshot simulado)
   f. No toca tablas de referencia (profiles, categories, rules, obligations)
4. Cockpit refresca evidencia → muestra datos demo frescos
```

**Dataset demo propuesto (SEED)**:

Movimientos (con source_reference para idempotencia, mezcla de PROJECTABLE y MANUAL_REVIEW):
| descripción | monto | dirección | fecha | status | categoría |
|---|---|---|---|---|---|
| Venta POS farmacia | 125000 | CREDIT | 2026-06-15 | PROJECTABLE | sales |
| Pago proveedor distribución | 88000 | DEBIT | 2026-06-16 | MANUAL_REVIEW | — |
| Abono transferencia cliente | 240000 | CREDIT | 2026-06-17 | MANUAL_REVIEW | — |
| Arriendo local junio | 900000 | DEBIT | 2026-06-05 | PROJECTABLE | rent |
| Compra mayorista laboratorio | 450000 | DEBIT | 2026-06-18 | MANUAL_REVIEW | — |
| Depósito efectivo caja | 63500 | CREDIT | 2026-06-19 | PROJECTABLE | sales |
| Pago servicios básicos | 250000 | DEBIT | 2026-06-12 | PROJECTABLE | utilities |

Sync session (santander, simulado):
```json
{
  "syncId": "demo-sync-santander-001",
  "providerType": "santander",
  "status": "COMPLETED",
  "pagesFetched": 1,
  "entriesFetched": 4,
  "importedEntries": 3,
  "hasMorePages": false,
  "durability": "DURABLE"
}
```

Cockpit preferences: `opening_balance = 350000`, `preferred_horizon_days = 7`.

**Dataset dimensionado para demostrar**:
- Movimientos para revisión (3 MANUAL_REVIEW) → ejercitar panel de revisión
- Movimientos proyectables (4 PROJECTABLE) → permite proyección inmediata
- Mix DEBIT/CREDIT → evidencia de dirección
- Sync session → comprobante visible
- Preferencias → cockpit pre-cargado

**Pros:**
- Mínima superficie de cambio: 1 puerto nuevo + 1 adapter + 1 endpoint opcional
- Cero cambios en domain/ — el puerto usa ProfileId y nombres agnósticos
- Reutiliza `CashflowMovementHistoryPort.saveAll()` existente para el seed (no INSERT raw)
- El seed produce movimientos con el pipeline completo de ingestión (dedup, categorización, sensitive data)
- La operación de reset es explícita y acotada: solo el perfil activo, solo tablas transaccionales
- Datos demo son determinísticos (mismos source_references → idempotentes si se re-ejecuta seed sin reset)
- Sin cambios en migraciones ni esquema de DB
- El cockpit puede verificar que el reset fue exitoso (refresh evidencia → muestra nuevo dataset)
- Separa claramente seed de reset — pueden usarse independientemente

**Cons:**
- El seed vía `CashflowIngestionService.ingest()` pasa por el pipeline completo (validación, categorización, sensitive data) — si la configuración de categorías o sensitive data cambia, el seed puede comportarse distinto
- El adapter necesita acceso a `CashflowIngestionService` (inyección de dependencia) — acoplamiento a application layer
- `DELETE FROM` directo es una operación destructiva — requiere validación explícita de que el perfil existe y está habilitado
- No hay trazabilidad de qué se borró (el adapter podría loguearlo)
- Las preferencias se pierden en el reset si se incluye `cockpit_preferences` — el usuario debe reingresar saldo inicial

**Effort:** Low-Medium (~400 líneas)

---

### 2. Columna `is_demo` + filtrado lógico

Agregar columna `is_demo BOOLEAN NOT NULL DEFAULT FALSE` a `cashflow_movement_history` y `provider_sync_sessions`. El seed marca filas con `is_demo = TRUE`. El reset hace `DELETE WHERE profile_id = ? AND is_demo = TRUE`. Las queries existentes no se modifican — el filtro `is_demo` solo lo usa el endpoint de reset.

**Pros:**
- Reset nunca toca datos reales — imposible borrar accidentalmente datos de producción
- Si en el futuro hay datos reales en la misma DB, el demo está aislado
- Fácil de auditar: `SELECT count(*) WHERE is_demo = TRUE`

**Cons:**
- Requiere migration (V7) — nueva columna en 2 tablas
- Dos caminos de datos (demo vs real) en las mismas tablas — complejidad cognitiva
- Las queries existentes deben seguir funcionando sin filtrar `is_demo` — el cockpit mostraría datos reales + demo mezclados a menos que se filtre explícitamente
- **Sobre-ingeniería para MVP sin auth/multi-user**: si no hay usuarios reales ni datos reales, ¿para qué distinguir?
- Rompe el principio YAGNI — la distinción demo/real es relevante solo cuando hay datos de producción

**Effort:** Medium (~500 líneas + migration)

---

### 3. Profile-level DELETE sin seed explícito (solo reset)

Solo se expone `POST /api/cockpit/demo/reset` que limpia las tablas transaccionales del perfil activo. El usuario debe manualmente hacer import y sync de nuevo (usando los botones existentes del cockpit). Esto es el "camino fácil" pero no cumple el requisito de "cargar datos demo para el perfil activo".

**Pros:**
- Mínimo código: un DELETE por tabla, sin lógica de seed
- Sin dataset hardcodeado en backend

**Cons:**
- **No cumple el requisito** — el usuario debe hacer 3+ acciones manuales para tener el cockpit funcional
- Depende de que los fixtures JSON y los SAMPLE_ROWS de `app.js` sigan siendo válidos
- El sync provider requiere llamada POST diferente a la importación — fricción
- Derrota el propósito de "demo repetible sin limpiar manualmente"

**Effort:** Very Low (~100 líneas) — pero **NO** recomendado porque no satisface la necesidad.

---

### 4. Migration repeatable con datos demo (estilo `R__seed_pharmacy_categories.sql`)

Una migration Flyway repeatable `R__seed_demo_data.sql` que inserta datos demo cada vez que la app arranca. Usa `INSERT ... ON CONFLICT DO NOTHING` para ser idempotente. El reset implica borrar la DB y reiniciar.

**Pros:**
- Sin código Java nuevo — solo SQL
- Flyway ya maneja el ciclo de vida

**Cons:**
- El reset requiere reiniciar la app — inviable para demo en vivo
- No hay endpoint — el cockpit no puede disparar el reset
- `ON CONFLICT DO NOTHING` significa que si el usuario ya categorizó un movimiento, el seed no lo "resetea" — el movimiento sigue en su estado modificado
- Las migraciones repeatable se ejecutan en cada startup — ruido en logs, potenciales colisiones
- **No recomendado** — Flyway es para DDL y reference data, no para datos transaccionales de demo

**Effort:** Very Low (~50 líneas SQL) — pero **NO** recomendado.

---

## Recommendation

**Approach 1 — `DemoDataPort` + `JdbcDemoDataAdapter` + endpoint REST** es la opción correcta para este MVP:

1. **Cumple el requisito completo**: seed carga datos demo, reset limpia para repetir, cockpit funcional después de cualquiera de las dos operaciones.
2. **Respeta la arquitectura hexagonal**: puerto en `application/port/out/` con nombres agnósticos, adapter en `infrastructure/demo/`, sin literales prohibidos en domain/application. ArchUnit pasa sin modificaciones.
3. **Independiente de fixtures externos**: el dataset está inline en el adapter Java — no depende de archivos JSON que puedan desincronizarse.
4. **Reutiliza el pipeline de ingestión**: el seed pasa por `CashflowIngestionService.ingest()` → validación, deduplicación, categorización, sensitive data. Los movimientos demo son indistinguibles de movimientos reales para el resto del sistema.
5. **Operaciones separables**: `reset` y `seed` son endpoints independientes. El cockpit puede llamar `seed` sin `reset` (añade más datos demo) o `reset` sin `seed` (limpia y deja vacío).
6. **Superficie de cambio acotada**: ~400 líneas en 2-3 chained PRs.

### Decisión sobre preferencias de cockpit

**Incluir `cockpit_preferences` en el reset pero restaurarlas en el seed.** El seed carga `opening_balance = 350000` y `preferred_horizon_days = 7`. Esto asegura que después de un `reset-and-seed`, el cockpit muestra el saldo inicial y período pre-cargados — el usuario puede proyectar inmediatamente.

### Decisión sobre scope de reset

**Solo el perfil activo** (`profile_id = 'pharmacy-cl'`). El endpoint recibe `profileId` y valida contra `VerticalProfileService.loadProfile()`. Si el perfil no existe o está deshabilitado, retorna 404/400.

### Decisión sobre sincronización de estado post-reset

Después del reset+seed, el cockpit DEBE refrescar toda la evidencia (movimientos, sync status, preferencias). Esto se maneja en el frontend — el `app.js` llama `refreshCockpitEvidence()` después de recibir respuesta exitosa del endpoint demo.

## Risks

1. **Destrucción accidental de datos (BAJA probabilidad, ALTO impacto)**: Si en el futuro hay múltiples perfiles o datos reales en la misma DB, un reset del perfil `pharmacy-cl` borraría todo. **Mitigación**: (a) El endpoint valida que el perfil existe y está habilitado. (b) El nombre del endpoint incluye `demo` explícitamente. (c) Documentar que esto es solo para MVP sin datos reales. (d) Si se agregan usuarios reales en el futuro, una migration con `is_demo` (Approach 2) se puede agregar sin romper el adapter actual.

2. **Idempotencia del seed (MEDIA probabilidad, BAJO impacto)**: Si se llama `seed` dos veces sin `reset` entre medio, los movimientos con mismo `source_reference` serán detectados como duplicados por `CashflowMovementHistoryPort.findBySourceReference()` y no se insertarán. La respuesta del seed debe indicar cuántos movimientos eran nuevos vs duplicados. **Mitigación**: el adapter puede devolver `seededCount` y `existingCount`.

3. **Categorización no determinística (MEDIA probabilidad, MEDIO impacto)**: Si se modifica la configuración de categorías (`application-vertical.yml`) o `SensitiveDataPolicy`, el seed puede producir resultados distintos (ej. movimientos que antes eran PROJECTABLE pasan a MANUAL_REVIEW). **Mitigación**: documentar el dataset esperado en el test del adapter. Si las categorías cambian, el test lo detecta.

4. **Transaccionalidad (BAJA probabilidad, MEDIO impacto)**: El reset (DELETE) y seed (INSERT vía ingestión) no ocurren en una sola transacción. Si el seed falla después del delete, la DB queda vacía. **Mitigación**: (a) El adapter puede wrap-ear ambas operaciones en `@Transactional`. (b) El endpoint combinado `reset-and-seed` minimiza la ventana. (c) El seed usa el pipeline de ingestión que ya es atómico por lote.

5. **Sync session como snapshot inmutable (BAJA probabilidad, BAJO impacto)**: `provider_sync_sessions` tiene un `UNIQUE INDEX (profile_id, provider_type)`. Si se elimina la sesión demo y se recrea con el mismo `syncId`, el índice único permite la inserción. **Mitigación**: el seed genera un `syncId` determinístico con prefijo `demo-`.

6. **ArchUnit — paquete `demo` (BAJA probabilidad, BAJO impacto)**: El adapter vive en `infrastructure/demo/`. Mientras no use literales prohibidos (`banco`, `bank`, etc.) en domain/application, ArchUnit pasa sin cambios. El nombre del paquete `demo` no es un literal restringido.

## Ready for Proposal

**Sí** — El exploration confirma:

- **No existe** mecanismo de seed/reset en el sistema actual.
- **Gap**: Sin demo seed/reset, repetir la demo requiere borrar la DB y reiniciar la app.
- **Approach**: `DemoDataPort` hexagonal + `JdbcDemoDataAdapter` + endpoint `POST /api/cockpit/demo/reset-and-seed` con dataset inline de 7 movimientos + 1 sync session + preferencias.
- **Arquitectura**: Cero cambios en `domain/`. Puerto agnóstico en `application/port/out/`. Adapter en `infrastructure/demo/`. Controller opcional en `interfaces/web/` (o integrado en el adapter como endpoint interno).
- **Tradeoffs documentados**: Preferencias se resetean y re-cargan, seed depende del pipeline de ingestión (no determinístico si cambian políticas), transaccionalidad entre DELETE e INSERT requiere `@Transactional`.
- **Review budget**: ~400 líneas, 2-3 chained PRs.

Decisiones clave requeridas antes de proposal:
- Confirmar Approach 1 (DemoDataPort + adapter) sobre Approach 2 (columna `is_demo`)
- Confirmar que `cockpit_preferences` se resetea y re-siembra con defaults
- Confirmar dataset demo propuesto (7 movimientos, 1 sync session, preferencias)
- Confirmar endpoint `POST /api/cockpit/demo/reset-and-seed` como operación combinada + endpoints separados `reset` y `seed` para flexibilidad
- Confirmar 2-3 chained PRs bajo el presupuesto de 400 líneas
