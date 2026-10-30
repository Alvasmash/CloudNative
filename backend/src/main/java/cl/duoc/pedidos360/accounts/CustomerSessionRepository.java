package cl.duoc.pedidos360.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerSessionRepository extends JpaRepository<CustomerSession, String> {
    long deleteByExpiresAtBefore(java.time.Instant now);
}
