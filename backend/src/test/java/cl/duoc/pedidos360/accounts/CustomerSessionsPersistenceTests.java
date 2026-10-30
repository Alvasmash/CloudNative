package cl.duoc.pedidos360.accounts;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.jpa.show-sql=false")
@Import(CustomerSessions.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CustomerSessionsPersistenceTests {
    @Autowired AccountRepository accounts;
    @Autowired CustomerSessions sessions;
    @Autowired PlatformTransactionManager transactions;

    @Test void committedSessionCanBeReadInANewTransactionAndRevoked() {
        String token = new TransactionTemplate(transactions).execute(status -> {
            Account a = new Account(); a.identityKey="local:persistence@example.com";
            a.email="persistence@example.com";a.nombre="Persistence";a.proveedor="CORREO";
            a.verificado=true;a.ultimoAcceso=Instant.now();accounts.saveAndFlush(a);
            return sessions.create(a).get("token").toString();
        });
        assertEquals("persistence@example.com", sessions.decode(token).getClaimAsString("email"));
        sessions.revoke(token);
        assertThrows(org.springframework.security.oauth2.jwt.JwtException.class, () -> sessions.decode(token));
    }
}
