package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cockpit.CockpitDemoResetService;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
public class DemoDataController {
    private final CockpitDemoResetService service;

    public DemoDataController(CockpitDemoResetService service) {
        this.service = service;
    }

    @PostMapping("/api/cockpit/demo/reset-and-seed")
    public CockpitDemoResetService.DemoResetResult resetAndSeed(@RequestParam @NotBlank String profileId) {
        return service.resetAndSeed(new ProfileId(profileId.trim()));
    }

    @ExceptionHandler(CockpitDemoResetService.DemoOnlyProfileException.class)
    ResponseEntity<ApiExceptionHandler.ApiErrorResponse> handleDemoOnlyProfile() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiExceptionHandler.ApiErrorResponse(
                "DEMO_ONLY_PROFILE_REQUIRED",
                "Esta acción está disponible solo para datos de demostración.",
                List.of()
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiExceptionHandler.ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(new ApiExceptionHandler.ApiErrorResponse(
                "VALIDATION_ERROR",
                "Revise los datos enviados e intente nuevamente.",
                List.of(new ApiExceptionHandler.ValidationErrorResponse("profileId", "El perfil es obligatorio."))
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ApiExceptionHandler.ApiErrorResponse> handleResetFailure(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiExceptionHandler.ApiErrorResponse(
                "DEMO_RESET_FAILED",
                "No fue posible reiniciar los datos de demostración.",
                List.of()
        ));
    }
}
