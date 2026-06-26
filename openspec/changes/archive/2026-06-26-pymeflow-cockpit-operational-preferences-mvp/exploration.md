# Exploration: Cockpit Operational Preferences MVP

## Current State

El cockpit PymeFlow (`/index.html` + `app.js`) presenta un formulario de proyección con controles de **saldo inicial manual** y **período de proyección (7 o 30 días)** que el usuario debe reingresar cada vez que carga la página. El perfil activo está hardcodeado como `"pharmacy-cl"` en `app.js`. No existe persistencia alguna para estas preferencias operacionales.

### Flujo actual

1. `app.js` define `const PROFILE_ID = "pharmacy-cl"` (línea 2)
2. El formulario HTML (`index.html` líneas 73-85) tiene:
   - `<input id="opening-balance">` sin valor por defecto
   - Radio buttons 7 y 30 días, con 7 días seleccionado por defecto
3. `handleProjectionSubmit()` (línea 107) lee `readOpeningBalance()` del DOM
4. `fetchProjection()` (línea 118) construye query params y llama `GET /api/cashflow/cockpit/projection`
5. Al refrescar (`refreshCockpitEvidence`, línea 390), solo re-ejecuta proyección si el input tiene valor actual — pero el valor se pierde al recargar la página

### Arquitectura hexagonal existente

```
domain/          → VerticalProfile, ProfileId, TenantId (POJOs puros, sin anotaciones de framework)
application/     → CockpitProjectionService, CashflowProjectionService, VerticalProfileService
  port/out/      → ProfileRegistryPort (loadProfile), CashflowMovementHistoryPort, etc.
infrastructure/  → VerticalProfileJpaAdapter (JdbcTemplate), ApplicationServiceConfiguration
interfaces/web/  → CashflowProjectionController, ProfileController
```

**Patrón de persistencia**: `JdbcTemplate` directo (sin JPA/ORM), migraciones Flyway, adaptadores que implementan ports.

### Base de datos

- Flyway V1-V5 existentes
- `vertical_profiles(id VARCHAR(63) PK, display_name, enabled, created_at)`
- Sin tabla de preferencias
- PostgreSQL en prod, H2 en test

## Affected Areas

| Área | Archivo | Por qué |
|------|---------|---------|
| **Domain** (nuevo) | `domain/cockpit/CockpitPreferences.java` | Value object puro: `record CockpitPreferences(ProfileId profileId, BigDecimal openingBalance, int preferredHorizonDays)` |
| **Port** (nuevo) | `application/port/out/CockpitPreferencesPort.java` | Interfaz con `save(CockpitPreferences)` y `load(ProfileId): Optional<CockpitPreferences>` |
| **Infrastructure** (nuevo) | `infrastructure/persistence/CockpitPreferencesJdbcAdapter.java` | Adaptador JDBC con upsert (`INSERT ... ON CONFLICT DO UPDATE`) |
| **Infrastructure** (modificar) | `infrastructure/config/ApplicationServiceConfiguration.java` | Nuevo `@Bean` para el adapter |
| **Interfaces** (nuevo) | `interfaces/web/CockpitPreferencesController.java` | `GET /api/cockpit/preferences?profileId=` y `PUT /api/cockpit/preferences` |
| **DB Migration** (nuevo) | `V6__create_cockpit_preferences.sql` | Tabla `cockpit_preferences(profile_id VARCHAR(63) PK FK, opening_balance NUMERIC(18,2), preferred_horizon_days INT DEFAULT 7)` |
| **Frontend** (modificar) | `app.js` y `index.html` | Carga inicial de preferencias, auto-guardado en cambio de controles, pre-llenado del formulario |
| **Tests** (nuevos) | Unit: `CockpitPreferencesJdbcAdapterTest`, `CockpitPreferencesControllerTest`. Static resource: verificar nuevo wiring en `CockpitStaticResourceTest` |

## Approaches

### 1. Tabla `cockpit_preferences` + modelo de dominio + endpoints REST

Persistencia real en PostgreSQL vía JDBC, siguiendo el patrón hexagonal completo. Una tabla pequeña con `profile_id` PK/FK, `opening_balance`, y `preferred_horizon_days`. El frontend carga preferencias al iniciar y las persiste al cambiar los controles (con debounce o en submit).

- **Pros**: Persistencia durable, sobrevive recargas y cambios de navegador, alineado con la arquitectura hexagonal existente, auditable, escala a multi-perfil naturalmente
- **Cons**: Requiere nuevo endpoint backend, nueva migración, nuevos tests (aunque pocos); más trabajo que localStorage
- **Effort**: Medium-Bajo

### 2. Solo localStorage en el frontend

Modificar únicamente `app.js` para guardar/cargar `openingBalance` y `preferredHorizonDays` desde `localStorage` (keyeado por perfil). Cero cambios en backend.

- **Pros**: Implementación inmediata, sin backend, sin migración, sin tests nuevos de backend
- **Cons**: Se pierde al cambiar de navegador/dispositivo, no alineado con la arquitectura de persistencia de la app, no escala a futuros requisitos de preferencias, inconsistente con el resto del sistema que persiste en BD
- **Effort**: Bajo

### 3. Valores por defecto en `application.yml` (`VerticalProfileProperties`)

Agregar campos `defaultOpeningBalance` y `defaultHorizonDays` a `VerticalProfileProperties`. El frontend los leería desde `/api/profiles/active` o un endpoint nuevo. No hay persistencia por perfil — solo defaults del sistema.

- **Pros**: Mínimo backend, centralizado en configuración
- **Cons**: No resuelve el problema del usuario (no puede guardar SUS valores, solo usa defaults), no es preferencia operacional real, confunde configuración de sistema con preferencia de usuario
- **Effort**: Muy bajo

## Recommendation

**Approach 1** es la opción correcta porque:

1. **El usuario pide persistir preferencias operacionales por perfil**. Solo la Approach 1 lo logra. Approach 2 es frágil (localStorage), Approach 3 no es preferencia real.
2. **Encaja perfectamente en la arquitectura hexagonal**: el patrón de domain record → port → JdbcAdapter está probado con `VerticalProfileJpaAdapter` y `ProfileRegistryPort`. La nueva preferencia sigue exactamente el mismo molde.
3. **Tabla mínima**: `cockpit_preferences` con 3 columnas (profile_id, opening_balance, preferred_horizon_days). Una sola migración Flyway V6.
4. **Copia honesta preservada**: la UI ya dice "Saldo inicial manual, no bancario" y "ingresado por el usuario". No se toca esa copia — solo se persiste el valor para no reingresarlo.
5. **No rompe nada existente**: el endpoint `/api/cashflow/cockpit/projection` sigue recibiendo `openingBalance` como query param; el nuevo endpoint de preferencias es independiente.
6. **Prepara para evolución**: si más adelante se agregan más preferencias (período por defecto, moneda, etc.), la tabla/modelo puede crecer sin cambiar la arquitectura.

### Flujo propuesto

```
[Usuario carga cockpit]
  → GET /api/cockpit/preferences?profileId=pharmacy-cl
  → Si existe: pre-llenar openingBalance y horizonDays en el formulario
  → Si no existe: formulario vacío con defaults (7 días, sin balance)

[Usuario cambia openingBalance o período]
  → PUT /api/cockpit/preferences { profileId, openingBalance, preferredHorizonDays }
  → Auto-guardado con debounce 500ms o en submit de proyección

[Usuario solicita proyección]
  → GET /api/cockpit/projection?profileId=...&openingBalance=...&horizonDays=...
  → Sin cambios en este endpoint
```

## Risks

- **Race condition en auto-guardado**: si el usuario cambia rápido los controles, múltiples PUTs pueden llegar en orden no determinístico. Mitigación: debounce de 500ms en el frontend, upsert idempotente en backend (`INSERT ... ON CONFLICT DO UPDATE`).
- **Preferencias sin tenant isolation**: actualmente no hay multi-tenant real (solo un perfil hardcodeado). Si se agrega multi-tenancy, `cockpit_preferences` necesitaría `tenant_id`. No es bloqueante para MVP — la tabla puede migrarse después.
- **El saldo inicial es BigDecimal**: hay que serializarlo como string en JSON para evitar pérdida de precisión. Los records de Spring Boot + Jackson manejan esto correctamente con la configuración por defecto.
- **Validación de horizonDays**: solo debe aceptar 7 o 30 para MVP. El controller debe validar con `@Pattern` o validación custom, la BD con `CHECK IN (7, 30)`.

## Ready for Proposal

**Sí**. La exploración confirma que el cambio es acotado, sigue patrones existentes, no rompe nada, y tiene un alcance claro. El orchestrator puede proceder a `sdd-propose`.

### Lo que el orchestrator debe decirle al usuario

"La exploración está lista. El cockpit actual te hace reingresar saldo inicial y período cada vez — eso es lo que vamos a resolver. La Approach 1 (tabla `cockpit_preferences` + endpoints REST) es la correcta: una tabla mínima de 3 columnas, un adapter JDBC, y el frontend auto-guardando tus preferencias. ¿Procedo con la propuesta formal?"
