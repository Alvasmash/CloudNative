package cl.duoc.pedidos360.controller;

import cl.duoc.pedidos360.dto.ProductoWriteDTO;
import cl.duoc.pedidos360.model.Producto;
import cl.duoc.pedidos360.service.ProductoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/productos")
public class AdminProductController {
    private final ProductoService products;
    public AdminProductController(ProductoService products) { this.products = products; }

    @GetMapping
    public List<Producto> listar() { return products.listarTodos(); }

    @PostMapping
    public ResponseEntity<Producto> crear(@Valid @RequestBody ProductoWriteDTO dto) {
        Producto product = products.crear(dto);
        return ResponseEntity.created(URI.create("/api/admin/productos/" + product.getId())).body(product);
    }

    @PutMapping("/{id}")
    public Producto editar(@PathVariable Long id, @Valid @RequestBody ProductoWriteDTO dto) {
        return products.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        products.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
