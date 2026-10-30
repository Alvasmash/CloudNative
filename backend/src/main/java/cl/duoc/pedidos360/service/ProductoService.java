package cl.duoc.pedidos360.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.pedidos360.model.Producto;
import cl.duoc.pedidos360.repository.ProductoRepository;

/**
 * =========================================================================
 * SERVICIO: ProductoService
 * =========================================================================
 * FUNCION: Contiene la logica de negocio para consultar productos del menu.
 * CONECTA CON: ProductoRepository (datos) y PublicController (endpoints publicos).
 */
@Service
@Transactional(readOnly = true) // Optimiza consultas de solo lectura en la base de datos
public class ProductoService {

    private final ProductoRepository productoRepository;

    // Inyeccin de dependencias por constructor (buena practica recomendada)
    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    // Retorna todo el catalogo de platos y bebidas
    public List<Producto> listarTodos() {
        return productoRepository.findByActivoTrueOrderByIdDesc();
    }

    // Busca un producto especifico por su ID
    public Optional<Producto> buscarPorId(Long id) {
        return productoRepository.findByIdAndActivoTrue(id);
    }

    // Filtra productos por categora (ej: "Pizzas")
    public List<Producto> listarPorCategoria(String categoria) {
        return productoRepository.findByCategoriaIgnoreCaseAndActivoTrue(categoria);
    }

    @Transactional
    public Producto crear(cl.duoc.pedidos360.dto.ProductoWriteDTO dto) {
        return productoRepository.save(aplicar(new Producto(), dto));
    }

    @Transactional
    public Producto actualizar(Long id, cl.duoc.pedidos360.dto.ProductoWriteDTO dto) {
        return productoRepository.save(aplicar(requerir(id), dto));
    }

    @Transactional
    public void eliminar(Long id) {
        // Preserve historical orders and prevent stale carts from purchasing this product.
        requerir(id).setActivo(false);
    }

    private Producto requerir(Long id) {
        return productoRepository.findActiveForUpdate(id).orElseThrow(() ->
            new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    private Producto aplicar(Producto producto, cl.duoc.pedidos360.dto.ProductoWriteDTO dto) {
        producto.setNombre(dto.nombre().trim());
        producto.setDescripcion(dto.descripcion().trim());
        producto.setPrecio(dto.precio());
        producto.setCategoria(dto.categoria().trim());
        producto.setStock(dto.stock());
        producto.setImagenUrl(dto.imagenUrl() == null || dto.imagenUrl().isBlank() ? null : dto.imagenUrl());
        return producto;
    }
}
