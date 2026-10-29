package cl.duoc.pedidos360;

import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.*;
import cl.duoc.pedidos360.security.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:microsoft-session-test;DB_CLOSE_DELAY=-1",
    "spring.sql.init.mode=never", "spring.rabbitmq.listener.simple.auto-startup=false", "app.kafka.enabled=false"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class LocalSessionTests {
    @Autowired MockMvc mvc;
    @Autowired AdminIdentityRepository identities;
    @MockitoBean JwtDecoder decoder;

    @BeforeEach void setUp() { identities.deleteAll(); }
    private void account(String token, String email, String id) {
        when(decoder.decode(token)).thenReturn(Jwt.withTokenValue(token).header("alg", "RS256")
            .subject(id).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
            .claim("tid", AdminEmailPolicy.TENANT).claim("oid", id).claim("name", "Cuenta Microsoft")
            .claim("preferred_username", email).claim("scp", "recurso.write").claim("roles", List.of("ADMIN")).build());
    }

    @Test void allThreeApprovedMicrosoftAccountsCanAdminister() throws Exception {
        String[] emails = {"benjaminJerez@pedidos360830.onmicrosoft.com", "alva.perezz@duocuc.cl", "ne.gonzalezm@duocuc.cl"};
        for (int i = 0; i < emails.length; i++) {
            String token = "approved-" + i;
            account(token, emails[i], "11111111-1111-1111-1111-11111111111" + i);
            mvc.perform(get("/api/session").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isAdmin").value(true));
            mvc.perform(get("/api/admin/kafka").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        }
    }

    @Test void everyOtherAccountIsUserEvenWhenTokenContainsAdminRole() throws Exception {
        account("other", "otro@duocuc.cl", "22222222-2222-2222-2222-222222222222");
        mvc.perform(get("/api/session").header("Authorization", "Bearer other"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.isAdmin").value(false));
        mvc.perform(get("/api/admin/pedidos").header("Authorization", "Bearer other")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/kafka").header("Authorization", "Bearer other")).andExpect(status().isForbidden());
    }

    @Test void removedDemoLoginCannotMintTokens() throws Exception {
        mvc.perform(post("/public/local-session").contentType(MediaType.APPLICATION_JSON)
            .content("{\"role\":\"ADMIN\"}")).andExpect(status().isNotFound());
    }

    @Test void invalidAndAnonymousSessionsCannotAccessApi() throws Exception {
        when(decoder.decode("old-demo-token")).thenThrow(new BadJwtException("invalid signature"));
        mvc.perform(get("/api/session").header("Authorization", "Bearer old-demo-token")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/pedidos")).andExpect(status().isUnauthorized());
    }

    @Test void recycledEmailCannotTakeOverPinnedAdministratorIdentity() throws Exception {
        account("original", "alva.perezz@duocuc.cl", "33333333-3333-3333-3333-333333333333");
        mvc.perform(get("/api/session").header("Authorization", "Bearer original"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.isAdmin").value(true));
        account("replacement", "alva.perezz@duocuc.cl", "44444444-4444-4444-4444-444444444444");
        mvc.perform(get("/api/admin/pedidos").header("Authorization", "Bearer replacement")).andExpect(status().isForbidden());
    }
}
