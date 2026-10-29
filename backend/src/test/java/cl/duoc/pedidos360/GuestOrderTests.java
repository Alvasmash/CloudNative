package cl.duoc.pedidos360;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:guest-tests;DB_CLOSE_DELAY=-1", "spring.sql.init.mode=never",
    "spring.rabbitmq.listener.simple.auto-startup=false", "app.kafka.enabled=false"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
@Transactional
class GuestOrderTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean RabbitTemplate rabbit;
    @MockitoBean JwtDecoder decoder;
    private static final String A = "A".repeat(43);
    private static final String B = "B".repeat(43);
    private static final String ORDER = "{\"clienteNombre\":\"Invitado\",\"clienteEmail\":\"benjaminJerez@pedidos360830.onmicrosoft.com\",\"items\":[{\"productoId\":1,\"cantidad\":1}]}";

    private long createGuest(String key) throws Exception {
        String result = mvc.perform(post("/public/guest/pedidos").header("X-Guest-Token", key)
            .contentType("application/json").content(ORDER)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.guestAccessHash").doesNotExist()).andExpect(jsonPath("$.ownerSubject").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(result).get("id").asLong();
    }

    @Test void guestCanCreateAndTrackWithoutMicrosoftLogin() throws Exception {
        long id = createGuest(A);
        mvc.perform(get("/public/guest/pedidos/" + id).header("X-Guest-Token", A))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
            .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/public/guest/pedidos").header("X-Guest-Token", A))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test void guestCannotReadAnotherGuestOrRegisteredOrder() throws Exception {
        long id = createGuest(A);
        mvc.perform(get("/public/guest/pedidos/" + id).header("X-Guest-Token", B)).andExpect(status().isNotFound());
        mvc.perform(get("/public/guest/pedidos/1").header("X-Guest-Token", A)).andExpect(status().isNotFound());
        mvc.perform(get("/public/guest/pedidos").header("X-Guest-Token", B))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void claimingAdminEmailDoesNotGiveGuestAnyAccountOrAdminPermission() throws Exception {
        createGuest(A);
        mvc.perform(get("/api/session").header("X-Guest-Token", A)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/pedidos").header("X-Guest-Token", A)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/kafka").header("X-Guest-Token", A)).andExpect(status().isUnauthorized());
    }

    @Test void missingAndMalformedGuestKeysAreRejected() throws Exception {
        mvc.perform(post("/public/guest/pedidos").contentType("application/json").content(ORDER)).andExpect(status().isBadRequest());
        mvc.perform(post("/public/guest/pedidos").header("X-Guest-Token", "admin")
            .contentType("application/json").content(ORDER)).andExpect(status().isBadRequest());
        mvc.perform(get("/public/guest/pedidos").header("X-Guest-Token", "bad")).andExpect(status().isBadRequest());
    }

    @Test void authenticatedUserCannotReadGuestOrdersByGuessingId() throws Exception {
        long id = createGuest(A);
        mvc.perform(get("/api/pedidos/" + id).with(jwt().jwt(t -> t.subject("regular-user"))))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/pedidos").with(jwt().jwt(t -> t.subject("regular-user"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/pedidos/" + id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk());
    }

    @Test void signedInOrdersBelongToTheTokenSubjectRatherThanTheFormEmail() throws Exception {
        String result = mvc.perform(post("/api/pedidos").with(jwt().jwt(t -> t.subject("owner-a"))
                .authorities(new SimpleGrantedAuthority("SCOPE_recurso.write")))
            .contentType("application/json").content(ORDER)).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long id = json.readTree(result).get("id").asLong();
        mvc.perform(get("/api/pedidos/" + id).with(jwt().jwt(t -> t.subject("owner-a")))).andExpect(status().isOk());
        mvc.perform(get("/api/pedidos/" + id).with(jwt().jwt(t -> t.subject("owner-b")))).andExpect(status().isNotFound());
        mvc.perform(get("/public/guest/pedidos/" + id).header("X-Guest-Token", A)).andExpect(status().isNotFound());
    }
}
