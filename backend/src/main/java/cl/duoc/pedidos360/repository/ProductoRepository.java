package cl.duoc.pedidos360.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.pedidos360.model.Producto;

/**
 * =========================================================================
 * REPOSITORIO JPA: ProductoRepository
 * =========================================================================
 * FUNCION: Provee operaciones de base de datos listas para la tabla 'productos' (buscar, guardar, borrar).
 * CONECTA CON: Spring Data JPA y la base de datos SQL.
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByActivoTrueOrderByIdDesc();
    java.util.Optional<Producto> findByIdAndActivoTrue(Long id);
    List<Producto> findByCategoriaIgnoreCaseAndActivoTrue(String categoria);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Producto p where p.id = :id and p.activo = true")
    java.util.Optional<Producto> findActiveForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    // Mtodo generado automaticamente por Spring Data JPA para filtrar platos por categora
    List<Producto> findByCategoriaIgnoreCase(String categoria);
}
