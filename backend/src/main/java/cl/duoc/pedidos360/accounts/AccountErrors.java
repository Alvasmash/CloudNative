package cl.duoc.pedidos360.accounts;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {AccountController.class, cl.duoc.pedidos360.controller.AdminProductController.class})
public class AccountErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> handle(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(java.util.Map.of("message",
            error.getReason() == null ? "No se pudo completar la operación." : error.getReason()));
    }
}
