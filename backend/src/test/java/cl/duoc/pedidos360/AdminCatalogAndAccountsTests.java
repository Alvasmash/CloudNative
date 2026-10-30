package cl.duoc.pedidos360;

import cl.duoc.pedidos360.accounts.*;
import cl.duoc.pedidos360.model.Producto;
import cl.duoc.pedidos360.repository.ProductoRepository;
import cl.duoc.pedidos360.producer.PedidoEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.kafka.enabled=false", "spring.datasource.url=jdbc:h2:mem:accounts-tests;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AdminCatalogAndAccountsTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AccountRepository accounts;
    @Autowired CustomerSessionRepository sessions;
    @Autowired ProductoRepository products;
    @MockitoBean JavaMailSender mail;
    @MockitoBean PedidoEventProducer producer;

    private final String password = "UnaClaveSegura123!";
    private final org.springframework.test.web.servlet.request.RequestPostProcessor admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));

    private String body(Object value) throws Exception { return json.writeValueAsString(value); }
    private Map<String, Object> product(String name, double price) {
        return Map.of("nombre", name, "descripcion", "Ingredientes de prueba", "precio", price,
            "categoria", "Especiales", "stock", 4, "imagenUrl", "images/product-3.jpg");
    }
    private String register(String email) throws Exception {
        mvc.perform(post("/public/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(body(Map.of("nombre", "Persona de prueba", "email", email, "password", password, "roles", List.of("ADMIN")))))
            .andExpect(status().isAccepted());
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, atLeastOnce()).send(message.capture());
        String text = message.getValue().getText();
        return text.substring(text.indexOf("?token=") + 7).split("\\s")[0];
    }
    private String login(String email) throws Exception {
        String response = mvc.perform(post("/public/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(body(Map.of("email", email, "password", password))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.isAdmin").value(false))
            .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    @Test void anonymousAndCustomersCannotManageProductsOrViewPeople() throws Exception {
        for (String url : List.of("/api/admin/productos", "/api/admin/usuarios")) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
            mvc.perform(get(url).with(jwt())).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/admin/productos").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(body(product("No autorizado", 1000))))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/productos/1").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(body(product("No autorizado", 1000))))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/productos/1").with(jwt())).andExpect(status().isForbidden());
    }

    @Test void adminCrudChangesPublicMenuAndDeletedProductCannotBeOrdered() throws Exception {
        String created = mvc.perform(post("/api/admin/productos").with(admin).contentType(MediaType.APPLICATION_JSON)
            .content(body(product("Nuevo plato", 4500)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        mvc.perform(get("/public/productos/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.precio").value(4500));
        mvc.perform(put("/api/admin/productos/" + id).with(admin).contentType(MediaType.APPLICATION_JSON).content(body(product("Plato editado", 5900))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Plato editado"));
        mvc.perform(delete("/api/admin/productos/" + id).with(admin)).andExpect(status().isNoContent());
        mvc.perform(get("/public/productos/" + id)).andExpect(status().isNotFound());
        assertFalse(products.findById(id).orElseThrow().isActivo());
        mvc.perform(post("/api/pedidos").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_recurso.write")))
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("clienteNombre", "Cliente", "clienteEmail", "cliente@example.com",
                "items", List.of(Map.of("productoId", id, "cantidad", 1)))))).andExpect(status().isBadRequest());
    }

    @Test void rejectsInvalidPricesStockAndUntrustedImagePaths() throws Exception {
        for (Map<String, Object> change : List.of(Map.<String,Object>of("precio", -1), Map.<String,Object>of("stock", -1),
                Map.<String,Object>of("imagenUrl", "https://untrusted.example/image.jpg"), Map.<String,Object>of("precio", 10.5))) {
            Map<String, Object> invalid = new HashMap<>(product("Producto", 1000)); invalid.putAll(change);
            mvc.perform(post("/api/admin/productos").with(admin).contentType(MediaType.APPLICATION_JSON).content(body(invalid))).andExpect(status().isBadRequest());
        }
    }

    @Test void registrationVerificationSessionAndRevocationWorkWithoutAdminPrivileges() throws Exception {
        String email = "registro@example.com";
        String token = register(email);
        Account a = accounts.findByIdentityKey("local:" + email).orElseThrow();
        assertNotEquals(password, a.passwordHash); assertNotEquals(token, a.verificationHash);
        mvc.perform(post("/public/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("email", email, "password", password))))
            .andExpect(status().isForbidden());
        mvc.perform(post("/public/auth/verify").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("token", token))))
            .andExpect(status().isOk());
        mvc.perform(post("/public/auth/verify").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("token", token))))
            .andExpect(status().isBadRequest());
        String bearer = login(email);
        mvc.perform(get("/api/session").header("Authorization", "Bearer " + bearer))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.isAdmin").value(false));
        mvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer " + bearer)).andExpect(status().isForbidden());
        String people = mvc.perform(get("/api/admin/usuarios?q=registro@example.com").with(admin))
            .andExpect(status().isOk()).andExpect(jsonPath("$.personas[0].rol").value("USER")).andReturn().getResponse().getContentAsString();
        assertFalse(people.contains("passwordHash")); assertFalse(people.contains("verificationHash")); assertFalse(people.contains(bearer));
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + bearer)).andExpect(status().isNoContent());
        mvc.perform(get("/api/session").header("Authorization", "Bearer " + bearer)).andExpect(status().isUnauthorized());
    }

    @Test void expiredVerificationCannotActivateAndResendingInvalidatesPreviousLink() throws Exception {
        String email = "expired@example.com"; String original = register(email);
        Account a = accounts.findByIdentityKey("local:" + email).orElseThrow(); a.verificationExpires = Instant.now().minusSeconds(60); accounts.saveAndFlush(a);
        mvc.perform(post("/public/auth/verify").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("token", original))))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/public/auth/resend").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("email", email))))
            .andExpect(status().isOk());
        mvc.perform(post("/public/auth/verify").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("token", original))))
            .andExpect(status().isBadRequest());
    }

    @Test void reservedAdminEmailsCannotRegisterAndForgedSessionsAreRejected() throws Exception {
        mvc.perform(post("/public/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(body(Map.of("nombre", "Impostor", "email", "alva.perezz@duocuc.cl", "password", password))))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/session").header("Authorization", "Bearer p360_" + "a".repeat(43))).andExpect(status().isUnauthorized());
    }

    @Test void expiredSessionsAreRejectedAndMailFailuresDoNotCreateUsableAccounts() throws Exception {
        String email = "expiry-session@example.com"; String verification = register(email);
        mvc.perform(post("/public/auth/verify").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("token", verification)))).andExpect(status().isOk());
        String bearer = login(email);
        CustomerSession session = sessions.findById(CustomerSessions.hash(bearer)).orElseThrow();
        session.expiresAt = Instant.now().minusSeconds(1); sessions.saveAndFlush(session);
        mvc.perform(get("/api/session").header("Authorization", "Bearer " + bearer)).andExpect(status().isUnauthorized());
        doThrow(new org.springframework.mail.MailSendException("SMTP unavailable")).when(mail).send(any(SimpleMailMessage.class));
        mvc.perform(post("/public/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(body(Map.of("nombre", "Persona", "email", "mail-fails@example.com", "password", password))))
            .andExpect(status().isServiceUnavailable());
        assertTrue(accounts.findByIdentityKey("local:mail-fails@example.com").isEmpty());
    }

    @Test void microsoftProfileIsListedWithItsServerAssignedRoleAndHistoricalOrdersKeepOriginalProductDetails() throws Exception {
        mvc.perform(get("/api/profile").with(jwt().jwt(t -> t.claim("tid", cl.duoc.pedidos360.security.AdminEmailPolicy.TENANT)
            .claim("oid", "99999999-8888-7777-6666-555555555555").claim("email", "persona@duocuc.cl").claim("name", "Persona Microsoft"))))
            .andExpect(status().isOk());
        mvc.perform(get("/api/admin/usuarios?q=persona@duocuc.cl").with(admin))
            .andExpect(jsonPath("$.personas[0].proveedor").value("MICROSOFT")).andExpect(jsonPath("$.personas[0].rol").value("USER"));
        Producto product = products.saveAndFlush(new Producto(null,"Nombre original","Descripción",1234.0,"Prueba",4));
        String order = mvc.perform(post("/api/pedidos").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_recurso.write")))
            .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("clienteNombre","Cliente","clienteEmail","cliente@example.com",
                "items",List.of(Map.of("productoId",product.getId(),"cantidad",1))))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(order).get("id").asLong();
        mvc.perform(put("/api/admin/productos/"+product.getId()).with(admin).contentType(MediaType.APPLICATION_JSON).content(body(product("Otro nombre",5000)))).andExpect(status().isOk());
        mvc.perform(delete("/api/admin/productos/"+product.getId()).with(admin)).andExpect(status().isNoContent());
        mvc.perform(get("/api/pedidos/"+id).with(admin)).andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].productoNombre").value("Nombre original"))
            .andExpect(jsonPath("$.items[0].precioUnitario").value(1234));
    }
}
