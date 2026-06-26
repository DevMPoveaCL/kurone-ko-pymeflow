# Exploration: Proyección de caja por período desde el cockpit

## Current State

### Proyección existente
- **Endpoint**: `POST /api/cashflow/projections` — recibe `openingBalance`, `startDate`, `horizonDays`, y lista manual de `ProjectedCashflowTransaction`. El caller debe enviar **todas** las transacciones. No lee desde historial persistido.
- **Motor**: `CashflowProjectionService.project(command)` — carga perfil (`VerticalProfileService`), resuelve categorías (`CashflowCategory.direction` = INFLOW/OUTFLOW/TRANSFER) y obligaciones (`ObligationTemplate` mensuales), itera día a día sumando inflows, restando outflows y obligaciones. Devuelve `CashflowProjectionResult`: `dailyBalances`, `closingProjectedBalance`, `appliedObligations`, `alerts`.
- **Semántica de dirección**: La proyección clasifica por `CashflowDirection` de la categoría (INFLOW/OUTFLOW), NO por `TransactionDirection` bancario (DEBIT/CREDIT). Ambas direcciones coexisten sin conflicto (spec `cashflow-direction-preservation`).

### Datos disponibles para alimentar proyección
- **Endpoint**: `GET /api/cashflow/history/projection-ready?profileId=X&startDate=Y&endDate=Z` — devuelve `ProjectionReadyCashflowTransaction` desde BD (status = PROJECTABLE, con `categoryKey`, `amount`, `date`, `direction`).
- **Conversión**: `ProjectionReadyCashflowTransaction.toProjectionTransaction()` produce `ProjectedCashflowTransaction` compatible con el comando de proyección.
- **Filtro por fecha**: El `CashflowMovementHistoryService.projectionReady()` ya acepta `startDate`/`endDate` opcionales y filtra en memoria. El adaptador JDBC (`findProjectionReady`) trae todos los PROJECTABLE del perfil; el filtro de fechas es post-query en el servicio.

### Cockpit actual (`app.js`)
- Obtiene `projection-ready` y `manual-review` vía API.
- Renderiza movimientos en ledger y panel de revisión.
- Calcula "Caja proyectada de hoy" como `sum(CREDIT) - sum(DEBIT)` sobre **todos** los movimientos cargados — **no usa el motor de proyección real**.
- **No tiene selector de período** (semana/mes/rango personalizado).
- **No llama al endpoint `POST /api/cashflow/projections`**.

### Perfil activo (`application-vertical.yml`)
- `pharmacy-cl`: 9 categorías (2 INFLOW, 7 OUTFLOW), 3 reglas de alerta, 3 obligaciones mensuales (proveedor día 10, arriendo día 5, remuneraciones día 30). Moneda CLP.

### Modelos de dominio relevantes
| Record | Campos clave |
|--------|-------------|
| `CashflowMovementRecord` | id, profileId, amount(+), currency, date, direction(DEBIT/CREDIT), status, categoryKey |
| `ProjectionReadyCashflowTransaction` | movementId, categoryKey, amount(+), currency, date, direction(DEBIT/CREDIT), status(PROJECTABLE) |
| `ProjectedCashflowTransaction` | categoryKey, amount(+), currency, date, direction(DEBIT/CREDIT) |
| `CashflowProjectionCommand` | profileId, openingBalance, currency, startDate, horizonDays, transactions |
| `CashflowProjectionResult` | dailyBalances, closingProjectedBalance, appliedObligations, alerts |
| `DailyProjectedBalance` | date, inflows, outflows, obligations, balance |

## Affected Areas

- `src/main/java/com/kuroneko/pymeflow/interfaces/web/CashflowProjectionController.java` — nuevo endpoint GET que orquesta proyección desde historial (o método adicional en controlador existente)
- `src/main/java/com/kuroneko/pymeflow/application/cashflow/CashflowProjectionService.java` — reutilizado sin cambios
- `src/main/java/com/kuroneko/pymeflow/application/cashflow/CashflowMovementHistoryService.java` — posible nuevo método `projectFromHistory()` o reutilizar `projectionReady()` existente
- `src/main/resources/static/app.js` — selector de período, llamada al nuevo endpoint, reemplazo del cálculo simplista de caja
- `src/main/resources/static/index.html` — markup para sección de proyección por período
- `src/main/resources/static/styles.css` — estilos para selector de período y tarjetas de resumen

## Approaches

### 1. Backend: nuevo endpoint GET de proyección desde historial
Crear `GET /api/cashflow/cockpit/projection?profileId=X&startDate=Y&horizonDays=N&openingBalance=B` que:
1. Lee `ProjectionReadyCashflowTransaction` del historial vía `CashflowMovementHistoryService.projectionReady()` con filtro de fechas
2. Convierte a `ProjectedCashflowTransaction` vía `.toProjectionTransaction()`
3. Construye `CashflowProjectionCommand` y llama al `CashflowProjectionService.project()` existente
4. Devuelve `CashflowProjectionResult` con balances diarios, abonos/cargos totales, cierre, obligaciones aplicadas y alertas

- **Pros**: Reutiliza 100% del motor de proyección existente; mínima lógica nueva; backend hexagonal — nuevo caso de uso en capa application; el cockpit solo consume y muestra; semántica DEBIT/CREDIT + positive amount preservada; obligaciones mensuales del perfil se aplican automáticamente
- **Cons**: Requiere nuevo endpoint (aunque es read-only); se necesita que el cockpit conozca o calcule `openingBalance`
- **Effort**: Medium

### 2. Frontend: ensamblar request en el cockpit y llamar POST existente
El cockpit ya obtiene `projection-ready` del endpoint GET. Ensamblar localmente el body del `POST /api/cashflow/projections` con las transacciones convertidas y llamarlo.

- **Pros**: Cero backend nuevo; usa endpoint existente
- **Cons**: Lógica de ensamblado en frontend vanilla JS (frágil); el cockpit debe conocer la estructura interna de `ProjectedCashflowTransaction` y `CashflowProjectionRequest`; duplica conversión que ya existe en Java (`toProjectionTransaction()`); no aprovecha obligaciones automáticas del perfil; `openingBalance` debe venir de algún lado
- **Effort**: Low

### 3. Híbrido: endpoint GET ligero + frontend simple
Backend expone un GET que devuelve transacciones projection-ready ya convertidas a formato de proyección + metadatos (openingBalance sugerido, rango de fechas disponible). Frontend arma el POST.

- **Pros**: Separación más limpia que opción 2; backend controla la conversión
- **Cons**: Dos llamadas (GET + POST); sigue requiriendo POST; más complejo que opción 1
- **Effort**: Medium

## Recommendation

**Approach 1** — nuevo endpoint GET `GET /api/cashflow/cockpit/projection` que orquesta proyección desde historial persistido.

**Razones**:
1. Menor acoplamiento: el cockpit no necesita conocer la mecánica interna de proyección ni duplicar lógica de conversión.
2. Reutilización máxima: `CashflowProjectionService`, `CashflowMovementHistoryService`, y `VerticalProfileService` ya existen.
3. Una sola llamada HTTP — menor latencia y complejidad en frontend vanilla.
4. El `openingBalance` puede ser capturado en el cockpit como input del usuario (campo "saldo inicial") o derivado de la suma de abonos - cargos visibles (como ya hace el cockpit actual).
5. La respuesta incluye obligaciones del perfil (arriendo, proveedor, remuneraciones) automáticamente — esto el frontend solo no podría hacer sin lógica de negocio duplicada.
6. Las alertas por regla (`projected_balance_below_threshold`, `obligations_due_before_cash_inflow`, `projected_balance_above_threshold`) se generan automáticamente — alto valor para el usuario PyME.

### Diseño mínimo del nuevo endpoint

```
GET /api/cashflow/cockpit/projection
  ?profileId=pharmacy-cl
  &startDate=2026-06-23
  &horizonDays=7
  &openingBalance=2500000
```

**Response**: Misma estructura que `CashflowProjectionResponse`:
- `dailyBalances[]`: date, inflows, outflows, obligations, balance
- `closingProjectedBalance`: CLP neto al final del horizonte
- `appliedObligations[]`: obligationKey, displayName, dueDate, amount
- `alerts[]`: ruleKey, actionKey, condition, date, balance

**Cockpit debe mostrar**:
- Resumen: "Caja proyectada al {fecha_fin}: ${closingProjectedBalance}"
- Totales del período: "Abonos: ${sum inflows}" / "Cargos: ${sum outflows}" / "Obligaciones: ${sum obligations}"
- Timeline de balances diarios (simple lista o mini-tabla)
- Alertas activas con texto en español neutro chileno
- Selector de período: "Esta semana" (7d), "Este mes" (30d), "Personalizado" (date pickers)

**Rollback**: Si el endpoint no está listo, el cockpit actual sigue funcionando con el cálculo simplista. El nuevo endpoint es puramente aditivo.

## Risks

- **Opening balance desconocido**: El cockpit no tiene un "saldo actual" persistido. Se mitiga pidiendo al usuario que ingrese su saldo inicial manualmente (campo en UI) o usando un default de 0 con advertencia visible.
- **Sin proyección sin movimientos PROJECTABLE**: Si no hay movimientos categorizados, el endpoint devolvería una proyección plana (solo obligaciones). El cockpit debe manejar este estado vacío con un mensaje claro ("Categoriza movimientos pendientes para ver tu proyección").
- **Fecha futura sin transacciones**: Si el horizonte incluye días sin transacciones, el balance diario solo reflejará obligaciones — esto es comportamiento correcto pero debe ser visible.
- **No hay endpoint para "saldo actual"**: El sistema no persiste un balance de cuenta. La proyección depende de `openingBalance` provisto por el usuario.

## Ready for Proposal

**Yes**. El camino está claro: un endpoint GET que orquesta datos existentes (projection-ready + perfil + obligaciones) a través del motor de proyección ya implementado. El trabajo se divide en backend (nuevo endpoint + servicio) y frontend (selector de período + visualización). Sin dependencias externas, sin nuevos modelos de dominio, sin cambios a la BD.
