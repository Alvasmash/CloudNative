package cl.duoc.pedidos360;

import cl.duoc.pedidos360.accounts.*;
import cl.duoc.pedidos360.producer.PedidoEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.kafka.enabled=false", "spring.datasource.url=jdbc:h2:mem:password-reset-tests;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PasswordRecoveryTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AccountRepository accounts;
    @Autowired CustomerSessionRepository sessions;
    @Autowired AccountService service;
    @MockitoBean JavaMailSender mail;
    @MockitoBean PedidoEventProducer producer;
    private final String oldPassword="AnteriorSegura123!", newPassword="UnaNuevaSegura456!";

    @BeforeEach void clean() { sessions.deleteAll(); accounts.deleteAll(); }
    private Account account(String email, boolean verified) {
        Account a=new Account();a.identityKey="local:"+email;a.email=email;a.nombre="Prueba recuperación";
        a.proveedor="CORREO";a.verificado=verified;a.passwordHash=new BCryptPasswordEncoder(4).encode(oldPassword);
        return accounts.saveAndFlush(a);
    }
    private String body(Object o) throws Exception { return json.writeValueAsString(o); }
    private String login(String email,String password,int expected) throws Exception {
        String result=mvc.perform(post("/public/auth/login").contentType("application/json")
            .content(body(Map.of("email",email,"password",password)))).andExpect(status().is(expected))
            .andReturn().getResponse().getContentAsString();
        return expected==200?json.readTree(result).get("token").asText():"";
    }
    private String requestToken(String email) throws Exception {
        mvc.perform(post("/public/auth/forgot-password").contentType("application/json").content(body(Map.of("email",email))))
            .andExpect(status().isAccepted()).andExpect(header().string("Cache-Control","no-store"));
        ArgumentCaptor<SimpleMailMessage> message=ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail,timeout(5000).atLeastOnce()).send(message.capture());
        String text=message.getAllValues().stream().filter(m->m.getSubject().startsWith("Recupera")).reduce((a,b)->b).orElseThrow().getText();
        String token=text.substring(text.indexOf("?token=")+7).split("\\s")[0];
        for(int i=0;i<100;i++){
            if(accounts.findByIdentityKey("local:"+email).map(a->CustomerSessions.hash(token).equals(a.resetHash)).orElse(false))return token;
            Thread.sleep(10);
        }
        throw new AssertionError("Reset email was sent but token was not committed");
    }
    private void reset(String token,String password,int expected) throws Exception {
        mvc.perform(post("/public/auth/reset-password").contentType("application/json").content(body(Map.of("token",token,"password",password))))
            .andExpect(status().is(expected));
    }

    @Test void changesPasswordOnceAndRevokesEveryPreviousSession() throws Exception {
        Account a=account("flow@example.com",true);
        String first=login(a.email,oldPassword,200),second=login(a.email,oldPassword,200);
        String token=requestToken(a.email);
        Account pending=accounts.findById(a.id).orElseThrow();
        assertNotEquals(token,pending.resetHash);assertTrue(pending.resetExpires.isAfter(Instant.now()));
        mvc.perform(get("/api/session").header("Authorization","Bearer "+first)).andExpect(status().isOk());
        reset(token,newPassword,200);
        login(a.email,oldPassword,401);String fresh=login(a.email,newPassword,200);
        for(String old:List.of(first,second))mvc.perform(get("/api/session").header("Authorization","Bearer "+old)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/session").header("Authorization","Bearer "+fresh)).andExpect(status().isOk()).andExpect(jsonPath("$.isAdmin").value(false));
        reset(token,"OtraClaveSegura789!",400);
        assertNull(accounts.findById(a.id).orElseThrow().resetHash);
        ArgumentCaptor<SimpleMailMessage> notice=ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail,timeout(3000).atLeast(2)).send(notice.capture());
        String notification=notice.getAllValues().stream().filter(m->m.getSubject().contains("cambió")).findFirst().orElseThrow().getText();
        assertFalse(notification.contains(oldPassword));assertFalse(notification.contains(newPassword));
    }

    @Test void expiredLinkDoesNotChangePasswordOrRevokeSessions() throws Exception {
        Account a=account("expired-reset@example.com",true);String bearer=login(a.email,oldPassword,200);
        String token=requestToken(a.email);Account pending=accounts.findById(a.id).orElseThrow();
        pending.resetExpires=Instant.now().minusSeconds(1);accounts.saveAndFlush(pending);
        reset(token,newPassword,400);login(a.email,oldPassword,200);
        mvc.perform(get("/api/session").header("Authorization","Bearer "+bearer)).andExpect(status().isOk());
    }

    @Test void secondRequestInvalidatesFirstAndValidationDoesNotConsumeValidLink() throws Exception {
        Account a=account("reissue@example.com",true);String first=requestToken(a.email);
        clearInvocations(mail);String second=requestToken(a.email);assertNotEquals(first,second);
        reset(first,newPassword,400);reset(second,"corta",400);reset(second,"á".repeat(40),400);reset(second,newPassword,200);
    }

    @Test void unknownUnverifiedAndMicrosoftOnlyAccountsGetTheSameResponseWithoutEmail() throws Exception {
        account("pending-reset@example.com",false);
        Account microsoft=new Account();microsoft.identityKey="microsoft:tenant:object";microsoft.email="admin@example.com";
        microsoft.nombre="Microsoft";microsoft.proveedor="MICROSOFT";microsoft.verificado=true;microsoft.administrador=true;accounts.saveAndFlush(microsoft);
        Set<String> responses=new HashSet<>();
        for(String email:List.of("unknown-reset@example.com","pending-reset@example.com","admin@example.com")){
            responses.add(mvc.perform(post("/public/auth/forgot-password").contentType("application/json").content(body(Map.of("email",email))))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString());
        }
        assertEquals(1,responses.size());verify(mail,after(300).never()).send(any(SimpleMailMessage.class));
        assertFalse(accounts.findByIdentityKey("local:pending-reset@example.com").orElseThrow().verificado);
    }

    @Test void mailFailureKeepsExistingLinkAndDoesNotChangePassword() throws Exception {
        Account a=account("smtp-reset@example.com",true);String token=requestToken(a.email);
        String existing=accounts.findById(a.id).orElseThrow().resetHash;
        doThrow(new org.springframework.mail.MailSendException("SMTP unavailable")).when(mail).send(any(SimpleMailMessage.class));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.requestPasswordReset(a.email));
        assertEquals(existing,accounts.findById(a.id).orElseThrow().resetHash);
        assertEquals(CustomerSessions.hash(token),existing);login(a.email,oldPassword,200);
    }

    @Test void repeatedRequestsAreLimitedAndForgedTokensCannotChangeCredentials() throws Exception {
        for(int i=0;i<5;i++)mvc.perform(post("/public/auth/forgot-password").contentType("application/json")
            .content(body(Map.of("email","limited-reset@example.com")))).andExpect(status().isAccepted());
        mvc.perform(post("/public/auth/forgot-password").contentType("application/json")
            .content(body(Map.of("email","limited-reset@example.com")))).andExpect(status().isTooManyRequests());
        reset("z".repeat(43),newPassword,400);
        mvc.perform(post("/public/auth/forgot-password").contentType("application/json").content(body(Map.of("email","not-an-email"))))
            .andExpect(status().isBadRequest());
    }
}
