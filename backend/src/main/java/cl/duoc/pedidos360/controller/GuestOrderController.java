package cl.duoc.pedidos360.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import cl.duoc.pedidos360.dto.PedidoCreateDTO;
import cl.duoc.pedidos360.model.Pedido;
import cl.duoc.pedidos360.service.GuestOrderService;

@RestController
@RequestMapping("/public/guest/pedidos")
public class GuestOrderController {
    private final GuestOrderService orders;
    public GuestOrderController(GuestOrderService orders) { this.orders = orders; }
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody PedidoCreateDTO dto, @RequestHeader("X-Guest-Token") String token) {
        try {
            Pedido order = orders.create(dto, token);
            return ResponseEntity.created(URI.create("/public/guest/pedidos/" + order.getId())).cacheControl(CacheControl.noStore()).body(order);
        } catch (IllegalArgumentException | IllegalStateException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }
    @GetMapping
    public ResponseEntity<List<Pedido>> list(@RequestHeader("X-Guest-Token") String token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(orders.list(token));
    }
    @GetMapping("/{id}")
    public ResponseEntity<Pedido> find(@PathVariable Long id, @RequestHeader("X-Guest-Token") String token) {
        return orders.find(id, token).map(order -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(order))
            .orElse(ResponseEntity.notFound().build());
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalidGuestToken() {
        return ResponseEntity.badRequest().body(Map.of("message", "Acceso de invitado no válido"));
    }
}
