package cl.duoc.pedidos360.accounts;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FrontendLinksTests {
    private final String token = "a".repeat(43);
    @Test void pagesEmailUsesRepositoryBaseAndHashRoute() {
        assertEquals("https://alvasmash.github.io/CloudNative/#/verificar-correo?token=" + token,
            new FrontendLinks("https://alvasmash.github.io/CloudNative/", true).verification(token));
    }
    @Test void localEmailKeepsItsCurrentRoute() {
        assertEquals("http://localhost:4200/verificar-correo?token=" + token,
            new FrontendLinks("http://localhost:4200", false).verification(token));
    }
    @Test void malformedOrUnsecuredPublicConfigurationIsRejected() {
        for (String url : new String[]{"http://public.example", "https://example.com/#/", "https://user:pass@example.com/", "https://example.com/?x=1"})
            assertThrows(IllegalArgumentException.class, () -> new FrontendLinks(url, true));
    }
}
