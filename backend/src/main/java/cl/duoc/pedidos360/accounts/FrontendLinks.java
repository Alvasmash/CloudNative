package cl.duoc.pedidos360.accounts;

import java.net.URI;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Only deployment configuration determines where verification links point. */
@Component
public class FrontendLinks {
    private final String base;
    private final boolean hashRouting;

    public FrontendLinks(@Value("${app.accounts.frontend-url:http://localhost:4200}") String configured,
            @Value("${app.accounts.hash-routing:false}") boolean hashRouting) {
        URI uri = URI.create(configured);
        boolean local = Set.of("localhost", "127.0.0.1", "[::1]").contains(String.valueOf(uri.getHost()));
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !("https".equals(uri.getScheme()) || (local && "http".equals(uri.getScheme()))))
            throw new IllegalArgumentException("FRONTEND_URL debe ser una URL HTTPS o localhost, sin credenciales, query ni fragmento.");
        this.base = configured.replaceAll("/+$", "") + "/";
        this.hashRouting = hashRouting;
    }

    public String verification(String token) {
        if (!token.matches("[A-Za-z0-9_-]{43}")) throw new IllegalArgumentException("Token de verificación no válido");
        return base + (hashRouting ? "#/" : "") + "verificar-correo?token=" + token;
    }
}
