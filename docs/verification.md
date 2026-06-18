# Verificación local de PymeFlow

Esta guía concentra los comandos mínimos para validar la API en entorno local sin cambiar el comportamiento de dominio.

## Ruta rápida

1. Use JDK 21 y Docker Desktop iniciado.
2. Ejecute la suite: `./gradlew.bat test --rerun-tasks`.
3. Genere cobertura: `./gradlew.bat jacocoTestReport`.
4. Levante PostgreSQL local: `docker compose up -d postgres`.
5. Inicie la app: `./gradlew.bat bootRun`.
6. Abra Swagger UI: `http://localhost:8080/swagger-ui.html`.

## Prerrequisitos

| Herramienta | Uso | Verificación |
|---|---|---|
| JDK 21 | Compilar y ejecutar la app | `java -version` |
| Gradle Wrapper | Ejecutar build y tests | `./gradlew.bat --version` |
| Docker Desktop | PostgreSQL local y pruebas de integración con PostgreSQL real | `docker info` |

Si `docker info` falla, inicie Docker Desktop y vuelva a ejecutar el comando antes de correr pruebas de integración.

## Tests y cobertura

```powershell
./gradlew.bat test --rerun-tasks
./gradlew.bat jacocoTestReport
```

El reporte HTML de JaCoCo queda en:

```text
build/reports/jacoco/test/html/index.html
```

Para validar la migración y los seeds de Flyway con PostgreSQL real:

```powershell
docker info
docker compose up -d postgres
./gradlew.bat test --tests "*FlywaySeedIntegrationTest" --rerun-tasks
```

Si Docker no está disponible, la falla corresponde al entorno local. Inicie Docker Desktop y repita la verificación.

## Ejecutar la app localmente

1. Levante PostgreSQL:

   ```powershell
   docker compose up -d postgres
   ```

2. Use las credenciales por defecto o ajuste variables si necesita otro datasource:

   ```text
   PYMEFLOW_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pymeflow
   PYMEFLOW_DATASOURCE_USERNAME=pymeflow
   PYMEFLOW_DATASOURCE_PASSWORD=pymeflow_local
   ```

3. Inicie la aplicación:

   ```powershell
   ./gradlew.bat bootRun
   ```

4. Confirme salud y perfil activo:

   ```text
   http://localhost:8080/actuator/health
   http://localhost:8080/api/profiles/active
   ```

## Prueba visual con Swagger/OpenAPI

Con la app iniciada, abra:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

Checklist de inspección:

- `GET /api/profiles/active` devuelve el perfil vertical activo.
- `GET /api/profiles/active/rules` muestra las reglas cargadas por Flyway.
- `GET /api/profiles/active/categories` muestra las categorías del perfil.
- `POST /api/cashflow/ingestions` permite probar ingesta simulada y devuelve `movementId` para el historial persistido seguro.
- `GET /api/cashflow/history/manual-review` lista movimientos pendientes de revisión manual por perfil.
- `POST /api/cashflow/manual-review/resolutions/{movementId}` resuelve una vez un movimiento persistido por id.
- `GET /api/cashflow/history/projection-ready` lista transacciones listas para usar en una proyección transitoria.
- `POST /api/cashflow/manual-review/resolutions` mantiene el flujo transitorio compatible, sin historial persistido.
- `POST /api/cashflow/projections` permite probar una proyección transitoria desde transacciones categorizadas.

Si Swagger UI no carga, revise primero los logs de arranque, la conexión a PostgreSQL y que la app esté escuchando en `localhost:8080`. No hace falta un frontend para esta verificación.

## Flujo persistido de historial de caja

Este flujo valida el MVP de historial persistido sin agregar lógica bancaria ni reglas específicas de un comercio. Los ejemplos usan `pharmacy-cl` como perfil vertical configurable y texto seguro de caja para el mercado chileno.

### 1. Ingestar movimientos y recibir `movementId`

Endpoint Swagger:

```text
http://localhost:8080/swagger-ui.html
POST /api/cashflow/ingestions
```

Payload de ejemplo:

```json
{
  "profileId": "pharmacy-cl",
  "transactions": [
    {
      "description": "Venta Caja 1",
      "amount": 125000,
      "currency": "CLP",
      "date": "2026-06-11"
    },
    {
      "description": "Movimiento sin clasificacion",
      "amount": 88000,
      "currency": "CLP",
      "date": "2026-06-11"
    }
  ]
}
```

Respuesta esperada para una transacción categorizada y otra pendiente de revisión manual:

```json
{
  "categorized": [
    {
      "movementId": "11111111-1111-1111-1111-111111111111",
      "transaction": {
        "description": "Venta Caja 1",
        "amount": 125000,
        "currency": "CLP",
        "date": "2026-06-11"
      },
      "category": {
        "key": "sales",
        "displayName": "Ventas",
        "direction": "INFLOW"
      }
    }
  ],
  "manualReview": [
    {
      "movementId": "22222222-2222-2222-2222-222222222222",
      "transaction": {
        "description": "Movimiento sin clasificacion",
        "amount": 88000,
        "currency": "CLP",
        "date": "2026-06-11"
      },
      "reason": "Requiere clasificación manual."
    }
  ],
  "rejected": []
}
```

Use el `movementId` del bloque `manualReview` para consultar y resolver el movimiento persistido. No copie descripciones sensibles ni transacciones rechazadas hacia la resolución.

### 2. Leer movimientos pendientes de revisión manual

Endpoint Swagger:

```text
http://localhost:8080/swagger-ui.html
GET /api/cashflow/history/manual-review?profileId=pharmacy-cl
```

Respuesta esperada:

```json
[
  {
    "movementId": "22222222-2222-2222-2222-222222222222",
    "amount": 88000,
    "currency": "CLP",
    "date": "2026-06-11",
    "description": "Movimiento sin clasificacion",
    "sourceReference": null,
    "status": "MANUAL_REVIEW"
  }
]
```

La respuesta contiene solo campos seguros persistidos. En este flujo de ingesta Swagger no se envía referencia de origen, por eso `sourceReference` queda `null`. No debe incluir descripciones sensibles, datos de salud, documentos, tarjetas ni otros identificadores personales.

### 3. Repetir ingesta con `externalReference` sin duplicar historial

Para hacer idempotente una transacción, envíe `externalReference` con un identificador externo seguro del cliente o sistema origen. No use RUT, tarjetas, datos de salud ni referencias sensibles.

Payload de ejemplo:

```json
{
  "profileId": "pharmacy-cl",
  "transactions": [
    {
      "description": "Venta Caja 1",
      "amount": 125000,
      "currency": "CLP",
      "date": "2026-06-11",
      "externalReference": "venta-caja-1-20260611-001"
    }
  ]
}
```

Si repite el mismo `externalReference` para el mismo perfil, la API debe devolver el `movementId` original y no crear un segundo movimiento, incluso si el payload de replay trae diferencias accidentales:

```json
{
  "profileId": "pharmacy-cl",
  "transactions": [
    {
      "description": "Venta Caja 1 corregida",
      "amount": 999999,
      "currency": "CLP",
      "date": "2026-06-12",
      "externalReference": "venta-caja-1-20260611-001"
    }
  ]
}
```

Respuesta esperada del replay: mismo `movementId`, estado y categoría del primer registro. Si `externalReference` se omite, la ingesta mantiene el comportamiento actual y puede crear un nuevo movimiento.

### 4. Resolver por id una sola vez

Endpoint Swagger:

```text
http://localhost:8080/swagger-ui.html
POST /api/cashflow/manual-review/resolutions/{movementId}
```

Payload de ejemplo para `22222222-2222-2222-2222-222222222222`:

```json
{
  "profileId": "pharmacy-cl",
  "chosenCategoryKey": "sales",
  "description": "Venta Caja 1",
  "sourceReference": "caja-1"
}
```

Respuesta esperada:

```json
{
  "transaction": {
    "movementId": "22222222-2222-2222-2222-222222222222",
    "categoryKey": "sales",
    "amount": 88000,
    "currency": "CLP",
    "date": "2026-06-11",
    "status": "PROJECTABLE"
  },
  "category": {
    "key": "sales",
    "displayName": "Ventas",
    "direction": "INFLOW"
  },
  "description": "Movimiento sin clasificacion",
  "sourceReference": null
}
```

La resolución por id valida `description` y `sourceReference` opcionales del payload para evitar texto sensible, pero no reemplaza el contexto seguro ya persistido durante la ingesta.

Si repite la resolución del mismo `movementId`, la API debe rechazarla con un mensaje neutral como `El movimiento ya fue resuelto o no está disponible para revisión manual.`.

### 5. Leer transacciones listas para proyección

Endpoint Swagger:

```text
http://localhost:8080/swagger-ui.html
GET /api/cashflow/history/projection-ready?profileId=pharmacy-cl&startDate=2026-06-01&endDate=2026-06-30
```

Respuesta esperada:

```json
[
  {
    "movementId": "11111111-1111-1111-1111-111111111111",
    "categoryKey": "sales",
    "amount": 125000,
    "currency": "CLP",
    "date": "2026-06-11",
    "status": "PROJECTABLE"
  },
  {
    "movementId": "22222222-2222-2222-2222-222222222222",
    "categoryKey": "sales",
    "amount": 88000,
    "currency": "CLP",
    "date": "2026-06-11",
    "status": "PROJECTABLE"
  }
]
```

Esta respuesta está preparada para construir el arreglo `transactions` de `POST /api/cashflow/projections`. No incluye `description` ni `sourceReference`.

### 5. Usar el historial proyectable en la proyección transitoria

Endpoint Swagger:

```text
http://localhost:8080/swagger-ui.html
POST /api/cashflow/projections
```

Payload de ejemplo:

```json
{
  "profileId": "pharmacy-cl",
  "openingBalance": 1500000,
  "currency": "CLP",
  "startDate": "2026-06-11",
  "horizonDays": 3,
  "transactions": [
    {
      "categoryKey": "sales",
      "amount": 125000,
      "currency": "CLP",
      "date": "2026-06-11",
      "status": "PROJECTABLE"
    },
    {
      "categoryKey": "sales",
      "amount": 88000,
      "currency": "CLP",
      "date": "2026-06-11",
      "status": "PROJECTABLE"
    }
  ]
}
```

La proyección sigue siendo transitoria: el historial guarda movimientos seguros y su estado, pero no persiste resultados de proyección.

### Notas de seguridad del historial persistido

- Use descripciones genéricas de caja, por ejemplo `Venta Caja 1`; no use nombres de personas, identificadores de salud, documentos, tarjetas ni otros datos sensibles.
- No persista ni use descripciones sensibles como insumo de proyección. Si una descripción sensible llega a la API, la respuesta no debe repetirla.
- Los movimientos rechazados o sensibles nunca deben aparecer en `GET /api/cashflow/history/projection-ready`.
- La resolución por id es de un solo uso: solo permite la transición `MANUAL_REVIEW` -> `PROJECTABLE`.
- `chosenCategoryKey` debe existir en las categorías configuradas del perfil. Verifique primero con `GET /api/profiles/active/categories`.

### Rechazo seguro por datos sensibles

Payload para probar rechazo por datos sensibles:

```json
{
  "profileId": "pharmacy-cl",
  "transactions": [
    {
      "description": "Venta Caja 2 receta",
      "amount": 42000,
      "currency": "CLP",
      "date": "2026-06-11"
    }
  ]
}
```

Respuesta esperada para una transacción rechazada por datos sensibles:

```json
{
  "categorized": [],
  "manualReview": [],
  "rejected": [
    {
      "movementId": "33333333-3333-3333-3333-333333333333",
      "amount": 42000,
      "currency": "CLP",
      "date": "2026-06-11",
      "reasonCode": "SENSITIVE_IDENTIFIER_REJECTED",
      "reason": "La transacción contiene datos sensibles y no fue clasificada."
    }
  ]
}
```

El texto sensible enviado en `description` no debe quedar en la respuesta ni en campos proyectables.

## Resolución manual transitoria

Endpoint Swagger:

```text
http://localhost:8080/swagger-ui.html
POST /api/cashflow/manual-review/resolutions
```

Flujo visual recomendado:

1. Ejecute `POST /api/cashflow/ingestions` con el ejemplo anterior y confirme que `Movimiento sin clasificacion` aparece en `manualReview`.
2. Revise `GET /api/profiles/active/categories` y elija una categoría existente del perfil. Para este ejemplo se usa `sales`.
3. Envíe la resolución manual. El resultado debe quedar con estado `PROJECTABLE`.
4. Copie el objeto `transaction` de la respuesta en `POST /api/cashflow/projections` para validar que ya puede usarse en una proyección transitoria.

Payload de ejemplo:

```json
{
  "profileId": "pharmacy-cl",
  "chosenCategoryKey": "sales",
  "amount": 88000,
  "currency": "CLP",
  "date": "2026-06-11",
  "description": "Venta Caja 1",
  "sourceReference": "caja-1",
  "sourceStatus": "MANUAL_REVIEW",
  "status": "PROJECTABLE"
}
```

Respuesta esperada:

```json
{
  "transaction": {
    "categoryKey": "sales",
    "amount": 88000,
    "currency": "CLP",
    "date": "2026-06-11",
    "status": "PROJECTABLE"
  },
  "category": {
    "key": "sales",
    "displayName": "Ventas",
    "direction": "INFLOW"
  },
  "description": "Venta Caja 1",
  "sourceReference": "caja-1"
}
```

Notas de seguridad:

- Use descripciones genéricas de caja, por ejemplo `Venta Caja 1`; no use nombres de personas, identificadores de salud, documentos, tarjetas ni otros datos sensibles.
- No resuelva transacciones del bloque `rejected` ni entradas marcadas como sensibles. Esas transacciones no deben convertirse en proyectables.
- `sourceStatus`, si se informa, debe ser `MANUAL_REVIEW`; `REJECTED` se rechaza.
- `status`, si se informa, debe ser `PROJECTABLE` o `CATEGORIZED`; `MANUAL_REVIEW` y `REJECTED` se rechazan.
- `chosenCategoryKey` debe existir en las categorías configuradas del perfil. Verifique primero con `GET /api/profiles/active/categories`.
- La respuesta es transitoria: no crea cola, historial ni registro persistido de resolución.

## Proyección transitoria de flujo de caja

Endpoint Swagger:

```text
http://localhost:8080/swagger-ui.html
POST /api/cashflow/projections
```

Payload de ejemplo:

```json
{
  "profileId": "pharmacy-cl",
  "openingBalance": 1500000,
  "currency": "CLP",
  "startDate": "2026-02-01",
  "horizonDays": 3,
  "transactions": [
    {
      "categoryKey": "sales",
      "amount": 125000,
      "currency": "CLP",
      "date": "2026-02-01",
      "status": "PROJECTABLE"
    },
    {
      "categoryKey": "sales",
      "amount": 88000,
      "currency": "CLP",
      "date": "2026-02-01",
      "status": "PROJECTABLE"
    },
    {
      "categoryKey": "suppliers",
      "amount": 75000,
      "currency": "CLP",
      "date": "2026-02-02"
    }
  ]
}
```

Respuesta esperada:

```json
{
  "dailyBalances": [
    {
      "date": "2026-02-01",
      "inflows": 213000,
      "outflows": 0,
      "obligations": 0,
      "balance": 1713000
    },
    {
      "date": "2026-02-02",
      "inflows": 0,
      "outflows": 75000,
      "obligations": 0,
      "balance": 1638000
    },
    {
      "date": "2026-02-03",
      "inflows": 0,
      "outflows": 0,
      "obligations": 0,
      "balance": 1638000
    }
  ],
  "closingProjectedBalance": 1638000,
  "appliedObligations": [],
  "alerts": []
}
```

Notas de validación visual:

- La proyección no solicita `description`; use solo categoría, monto, moneda, fecha y estado proyectable opcional.
- La moneda soportada para estos ejemplos es `CLP` y debe coincidir entre la proyección y las transacciones.
- Si informa `status`, use `PROJECTABLE` o `CATEGORIZED`; estados como `MANUAL_REVIEW` o `REJECTED` se rechazan antes de proyectar.
- Las categorías deben existir en el perfil activo; revise `GET /api/profiles/active/categories` antes de probar nuevos casos.

## Cobertura de escenarios de proyección

| Escenario SDD | Cobertura |
|---|---|
| Proyectar desde transacciones categorizadas explícitas | `CashflowProjectionControllerTest.returnsProjectionResponseShape`, `CashflowProjectionServiceTest.projectsDailyBalancesFromOpeningBalanceAndCategorizedMovements` |
| Omitir descripciones sensibles | `CashflowProjectionControllerTest.acceptsProjectionWithoutSensitiveDescriptions` |
| Serie diaria y saldo de cierre | `CashflowProjectionControllerTest.returnsProjectionResponseShape`, `CashflowProjectionServiceTest.projectsDailyBalancesFromOpeningBalanceAndCategorizedMovements` |
| Alertas por reglas del perfil | `CashflowProjectionServiceTest.createsAlertsForSupportedProfileRulesAndIgnoresUnknownConditions` |
| Obligaciones configuradas por perfil | `CashflowProjectionServiceTest.appliesMonthlyProfileObligationsToDueDateBalances` |
| Día 31 en meses cortos | `CashflowProjectionServiceTest.appliesShortMonthDueDayOnLastValidDay` |
| Perfil desconocido | `CashflowProjectionControllerTest.returnsUnknownProfileAsNeutralSpanishBadRequest`, `CashflowProjectionServiceTest.rejectsUnknownCategoriesProfilesAndCurrencyMismatches` |
| Horizonte inválido | `CashflowProjectionControllerTest.returnsInvalidHorizonValidationError` |
| Diferencia de moneda | `CashflowProjectionControllerTest.returnsCurrencyMismatchAsNeutralSpanishBadRequest`, `CashflowProjectionServiceTest.rejectsUnknownCategoriesProfilesAndCurrencyMismatches` |
| Transacciones manuales o rechazadas no proyectables | `CashflowProjectionControllerTest.rejectsManualAndRejectedStatusesAtInterfaceBoundary` |

## Cobertura de escenarios de resolución manual

| Escenario SDD | Cobertura |
|---|---|
| Resolver un movimiento en revisión manual de forma transitoria | `ManualReviewResolutionControllerTest.resolvesManualReviewMovementIntoProjectionCompatibleResponse`, `ManualReviewResolutionServiceTest.resolvesManualReviewMovementIntoProjectableTransaction` |
| Repetir una resolución sin lookup persistido | `ManualReviewResolutionServiceTest.treatsRepeatedResolutionAsStatelessRequest` |
| Solicitud mínima aceptada sin descripción ni referencia | `ManualReviewResolutionControllerTest.omitsOptionalContextWhenServiceReturnsNoSafeText`, `ManualReviewResolutionServiceTest.resolvesMinimalRequestWithoutDescriptionOrReference` |
| Perfil o categoría desconocida | `ManualReviewResolutionControllerTest.returnsUnknownProfileAndCategoryAsNeutralSpanishErrors`, `ManualReviewResolutionServiceTest.rejectsUnknownProfileOrCategory` |
| Descripción o referencia sensible rechazada sin eco | `ManualReviewResolutionControllerTest.rejectsSensitiveDescriptionWithoutEchoingRequestText`, `ManualReviewResolutionControllerTest.rejectsSensitiveSourceReferenceWithoutEchoingRequestText`, `ManualReviewResolutionServiceTest.rejectsSensitiveDescriptionOrSourceReference` |
| Estados `REJECTED` o `MANUAL_REVIEW` no proyectables | `ManualReviewResolutionControllerTest.rejectsRejectedOrInvalidStatusMisuseAtInterfaceBoundary`, `ManualReviewResolutionServiceTest.rejectsRejectedSourceAndManualReviewOrRejectedOutputStatus` |
| Campos financieros inválidos | `ManualReviewResolutionControllerTest.returnsValidationErrorsForInvalidAmountCurrencyAndDate`, `ManualReviewResolutionServiceTest.rejectsInvalidFinancialFields` |

## Cobertura de escenarios de historial de caja

| Escenario SDD | Cobertura |
|---|---|
| Persistir movimiento proyectable con `movementId` | `CashflowIngestionControllerTest.returnsCategorizedTransaction`, `CashflowIngestionServiceTest.persistsCategorizedManualReviewAndRejectedOutcomesWithSafeFields` |
| Persistir movimiento pendiente de revisión manual | `CashflowIngestionControllerTest.returnsManualReviewTransaction`, `CashflowMovementHistoryServiceTest.returnsOnlyPendingManualReviewMovementsWithSafeFields` |
| Rechazar texto sensible durable y no hacer eco | `CashflowIngestionControllerTest.returnsSensitiveRejectionWithoutEchoingSensitiveDescription`, `CashflowIngestionServiceTest.persistsCategorizedManualReviewAndRejectedOutcomesWithSafeFields` |
| Leer pendientes por perfil con campos seguros | `CashflowHistoryControllerTest.listsPendingManualReviewMovementsWithSafeFieldsOnly`, `CashflowMovementHistoryJdbcAdapterTest.listsOnlyPendingManualReviewsForProfile` |
| Resolver por id una sola vez | `ManualReviewResolutionControllerTest.resolvesPersistedManualReviewMovementById`, `CashflowMovementHistoryServiceTest.resolvesPendingManualReviewByIdIntoProjectionReadyTransaction`, `CashflowMovementHistoryJdbcAdapterTest.resolvesPendingManualReviewWithAtomicStatusTransition` |
| Rechazar id desconocido, categoría inválida o movimiento no disponible | `ManualReviewResolutionControllerTest.mapsPersistedResolutionFailuresToNeutralSpanishErrors`, `CashflowMovementHistoryServiceTest.rejectsUnknownMovementDoubleResolutionRejectedAndInvalidCategory` |
| Excluir manuales, rechazados y sensibles de projection-ready | `CashflowHistoryControllerTest.listsProjectionReadyTransactionsCompatibleWithProjectionInput`, `CashflowMovementHistoryJdbcAdapterTest.listsOnlyProjectionReadyMovementsForProfile`, `CashflowMovementHistoryJdbcAdapterTest.rejectedMovementCannotBeResolvedAndResolvedMovementBecomesProjectionReady` |
| Mantener proyección transitoria sin persistir resultados | `CashflowProjectionControllerTest.returnsProjectionResponseShape`, `CashflowProjectionServiceTest.projectsDailyBalancesFromOpeningBalanceAndCategorizedMovements` |
