package cl.duoc.pedidos360;

import java.util.Arrays;
import cl.duoc.analytics.SalesAnalyticsApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * =========================================================================
 * CLASE PRINCIPAL: Pedidos360BackendApplication
 * =========================================================================
 * FUNCION: Punto de entrada principal para iniciar el backend Spring Boot.
 *          La anotacion @SpringBootApplication activa:
 *          1. @Configuration: Permite registrar componentes y beans.
 *          2. @EnableAutoConfiguration: Configura Tomcat, Spring Security y JPA automaticamente.
 *          3. @ComponentScan: Escanea todos los paquetes hijos para registrar Controllers, Services y Repositories.
 */
@SpringBootApplication
public class Pedidos360BackendApplication {

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--analytics")) {
            SalesAnalyticsApplication.run(args);
            return;
        }
        SpringApplication.run(Pedidos360BackendApplication.class, args);
    }
}
