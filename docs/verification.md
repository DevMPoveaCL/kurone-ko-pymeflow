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
- `POST /api/cashflow/ingestions` permite probar ingesta simulada sin persistir transacciones.
- `POST /api/cashflow/manual-review/resolutions` permite convertir un movimiento en revisión manual en una transacción proyectable transitoria.
- `POST /api/cashflow/projections` permite probar una proyección transitoria desde transacciones categorizadas.

Si Swagger UI no carga, revise primero los logs de arranque, la conexión a PostgreSQL y que la app esté escuchando en `localhost:8080`. No hace falta un frontend para esta verificación.

## Ingesta simulada de caja

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

Respuesta esperada para una transacción categorizada:

```json
{
  "categorized": [
    {
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
  "manualReview": [],
  "rejected": []
}
```

Respuesta esperada para una transacción que requiere revisión manual:

```json
{
  "categorized": [],
  "manualReview": [
    {
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

Use este resultado como punto de partida para la resolución manual. No copie descripciones sensibles ni transacciones rechazadas hacia la resolución; solo resuelva movimientos del bloque `manualReview`.

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
      "amount": 42000,
      "currency": "CLP",
      "date": "2026-06-11",
      "reasonCode": "SENSITIVE_IDENTIFIER_REJECTED",
      "reason": "La transacción contiene datos sensibles y no fue clasificada."
    }
  ]
}
```

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
