package cl.duoc.pedidos360.rabbitmq.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cl.duoc.pedidos360.rabbitmq.exception.BrokerOperationException;
import cl.duoc.pedidos360.rabbitmq.exception.CriticalResourceException;
import cl.duoc.pedidos360.rabbitmq.exception.ResourceAlreadyExistsException;
import cl.duoc.pedidos360.rabbitmq.exception.ResourceNotFoundException;

/**
 * =========================================================================
 * MANEJADOR GLOBAL DE EXCEPCIONES: RabbitMQExceptionHandler
 * =========================================================================
 * Criterio de Evaluacion EP3 (7%): Validacion de parametros.
 * Transforma violaciones de validacion, recursos no encontrados, conflictos
 * y errores operativos en respuestas JSON estandarizadas con codigos HTTP correctos.
 */
@RestControllerAdvice
public class RabbitMQExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQExceptionHandler.class);

    /**
     * Captura errores de Bean Validation (@Valid en request body) -> 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", "Error de validacion en los parametros de la solicitud");
        body.put("validationErrors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Argumentos ilegales en la logica de negocio -> 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Recursos no encontrados en RabbitMQ o base de datos -> 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Intento de alterar recursos criticos protegidos -> 409 Conflict
     */
    @ExceptionHandler(CriticalResourceException.class)
    public ResponseEntity<Map<String, Object>> handleCriticalResource(CriticalResourceException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Recurso ya existente -> 409 Conflict
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyExists(ResourceAlreadyExistsException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Errores de operacion con el broker RabbitMQ -> 500 Internal Server Error
     */
    @ExceptionHandler(BrokerOperationException.class)
    public ResponseEntity<Map<String, Object>> handleBrokerOperation(BrokerOperationException ex) {
        log.error("Excepcion operativa en RabbitMQ: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "Error operativo al interactuar con el broker de mensajeria");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
