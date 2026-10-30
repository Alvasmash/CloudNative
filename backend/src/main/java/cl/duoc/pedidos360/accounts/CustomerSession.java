package cl.duoc.pedidos360.accounts;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customer_sessions")
public class CustomerSession {
    @Id public String tokenHash;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) public Account account;
    @Column(nullable = false) public Instant expiresAt;
}
