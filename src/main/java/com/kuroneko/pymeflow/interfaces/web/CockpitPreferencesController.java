package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cockpit.CockpitPreferencesService;
import com.kuroneko.pymeflow.domain.cockpit.CockpitPreferences;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cashflow/cockpit/preferences")
@Tag(name = "Cockpit preferences")
@Validated
public class CockpitPreferencesController {
    private static final int DEFAULT_HORIZON_DAYS = 7;
    private static final String MANUAL_BALANCE_SOURCE = "USER_ENTERED_MANUAL";

    private final CockpitPreferencesService service;

    public CockpitPreferencesController(CockpitPreferencesService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
            summary = "Cargar preferencias operativas del cockpit",
            description = "Devuelve preferencias por perfil. El saldo, si existe, es manual e ingresado por el usuario."
    )
    public ResponseEntity<CockpitPreferencesResponse> load(
            @RequestParam(required = false)
            @NotBlank(message = "El perfil es obligatorio.")
            String profileId
    ) {
        return ResponseEntity.ok(service.load(new ProfileId(profileId))
                .map(CockpitPreferencesResponse::from)
                .orElseGet(CockpitPreferencesResponse::defaults));
    }

    @PutMapping
    @Operation(
            summary = "Guardar preferencias operativas del cockpit",
            description = "Persiste saldo manual ingresado por el usuario y horizonte preferido por perfil."
    )
    public ResponseEntity<CockpitPreferencesResponse> save(@Valid @RequestBody CockpitPreferencesRequest request) {
        validateInterfaceRules(request);
        var saved = service.save(new ProfileId(request.profileId()), request.openingBalance(), request.preferredHorizonDays());
        return ResponseEntity.ok(CockpitPreferencesResponse.from(saved));
    }

    private static void validateInterfaceRules(CockpitPreferencesRequest request) {
        if (request.preferredHorizonDays() != 7 && request.preferredHorizonDays() != 30) {
            throw new ApiExceptionHandler.ApiValidationException(List.of(
                    new ApiExceptionHandler.ValidationErrorResponse("preferredHorizonDays", "El horizonte debe ser 7 o 30 días.")
            ));
        }
        if (integerDigits(request.openingBalance()) > 16 || Math.max(request.openingBalance().scale(), 0) > 2) {
            throw new ApiExceptionHandler.ApiValidationException(List.of(
                    new ApiExceptionHandler.ValidationErrorResponse("openingBalance", "El saldo inicial debe ser un valor seguro.")
            ));
        }
    }

    private static int integerDigits(BigDecimal value) {
        return value.precision() - value.scale();
    }

    public record CockpitPreferencesRequest(
            @NotBlank(message = "El perfil es obligatorio.")
            @Schema(example = "pharmacy-cl")
            String profileId,

            @NotNull(message = "El saldo inicial es obligatorio.")
            @PositiveOrZero(message = "El saldo inicial no puede ser negativo.")
            @Schema(example = "350000")
            BigDecimal openingBalance,

            @NotNull(message = "El horizonte es obligatorio.")
            @Schema(example = "7", allowableValues = {"7", "30"})
            Integer preferredHorizonDays
    ) {
    }

    public record CockpitPreferencesResponse(
            BigDecimal openingBalance,
            int preferredHorizonDays,
            String balanceSource
    ) {
        static CockpitPreferencesResponse defaults() {
            return new CockpitPreferencesResponse(null, DEFAULT_HORIZON_DAYS, MANUAL_BALANCE_SOURCE);
        }

        static CockpitPreferencesResponse from(CockpitPreferences preferences) {
            return new CockpitPreferencesResponse(
                    preferences.openingBalance(),
                    preferences.preferredHorizonDays(),
                    MANUAL_BALANCE_SOURCE
            );
        }
    }
}
