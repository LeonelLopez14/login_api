package com.castagno.dev.login_api.exception;

import com.castagno.dev.login_api.dto.response.ErrorResponse;
import com.castagno.dev.login_api.exception.custom.InvalidCredentialsException;
import com.castagno.dev.login_api.exception.custom.UserAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice  // Intercepta excepciones de todos los controllers
public class GlobalExceptionHandler {

    // ─── Usuario ya existe (register) ────────────────────────────────
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
            UserAlreadyExistsException ex) {

        return buildResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    // ─── Credenciales incorrectas (login) ─────────────────────────────
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex) {

        return buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    // ─── Acceso denegado (rol insuficiente) ───────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "No tenés permisos para acceder a este recurso"
        );
    }

    // ─── Validaciones fallidas (@Valid) ───────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        // Construye un mapa campo → mensaje de error
        // Ej: { "email": "El formato del email no es válido" }
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getAllErrors()
                .forEach(error -> {
                    String field = ((FieldError) error).getField();
                    String message = error.getDefaultMessage();
                    errors.put(field, message);
                });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    // ─── Cualquier otra excepción no contemplada ──────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Ocurrió un error inesperado"  // No exponemos el mensaje real al cliente
        );
    }

    // ─── Método privado para construir el ErrorResponse ──────────────
    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String error, String message) {

        ErrorResponse response = ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(response);
    }
}
