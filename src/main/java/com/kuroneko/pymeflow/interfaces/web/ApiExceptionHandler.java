package com.kuroneko.pymeflow.interfaces.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationErrorResponse(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ValidationErrorResponse::field))
                .toList();

        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "VALIDATION_ERROR",
                "Revise los datos enviados e intente nuevamente.",
                errors
        ));
    }

    @ExceptionHandler(ApiValidationException.class)
    ResponseEntity<ApiErrorResponse> handleApiValidation(ApiValidationException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "VALIDATION_ERROR",
                "Revise los datos enviados e intente nuevamente.",
                exception.errors().stream()
                        .sorted(Comparator.comparing(ValidationErrorResponse::field))
                        .toList()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiErrorResponse(
                "BAD_REQUEST",
                messageFor(exception),
                List.of()
        ));
    }

    private static String messageFor(IllegalArgumentException exception) {
        var message = exception.getMessage();
        if (message == null) {
            return "No fue posible procesar la solicitud con los datos enviados.";
        }
        if (message.startsWith("Profile not found")) {
            return "El perfil solicitado no está configurado.";
        }
        if (message.startsWith("Unknown category")) {
            return "La categoría enviada no está configurada para el perfil.";
        }
        if (message.contains("perfil indicado no está configurado")) {
            return "El perfil solicitado no está configurado.";
        }
        if (message.contains("categoría seleccionada no existe")) {
            return "La categoría enviada no está configurada para el perfil.";
        }
        if (message.contains("información sensible")) {
            return "La información enviada contiene datos sensibles y no puede proyectarse.";
        }
        if (message.contains("fecha final no puede ser anterior")) {
            return "La fecha final no puede ser anterior a la fecha inicial.";
        }
        if (message.contains("movimiento rechazado") || message.contains("revisión manual") || message.contains("listo para proyección") || message.contains("estado de salida")) {
            return "Solo se pueden resolver movimientos en revisión manual hacia un resultado listo para proyección.";
        }
        if (message.contains("monto")) {
            return "El monto debe ser mayor que cero.";
        }
        if (message.contains("moneda")) {
            return "La moneda es obligatoria y debe estar soportada.";
        }
        if (message.contains("fecha")) {
            return "La fecha es obligatoria y debe ser válida.";
        }
        if (message.toLowerCase().contains("currency")) {
            return "La moneda de las transacciones debe coincidir con la moneda de la proyección.";
        }
        if (message.toLowerCase().contains("horizon")) {
            return "La fecha de las transacciones debe estar dentro del horizonte de proyección.";
        }
        return "No fue posible procesar la solicitud con los datos enviados.";
    }

    public record ApiErrorResponse(String code, String message, List<ValidationErrorResponse> errors) {
    }

    public record ValidationErrorResponse(String field, String message) {
    }

    static class ApiValidationException extends RuntimeException {
        private final List<ValidationErrorResponse> errors;

        ApiValidationException(List<ValidationErrorResponse> errors) {
            this.errors = List.copyOf(errors);
        }

        List<ValidationErrorResponse> errors() {
            return errors;
        }
    }
}
