package cl.duoc.pedidos360.config;

import javax.sql.DataSource;
import cl.duoc.pedidos360.repository.ProductoRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
@Profile("local")
public class LocalCatalogConfig {
    @Bean
    ApplicationRunner seedLocalCatalog(DataSource source, ProductoRepository products) {
        return args -> {
            if (products.count() == 0) {
                new ResourceDatabasePopulator(new ClassPathResource("data.sql")).execute(source);
            }
        };
    }
}
