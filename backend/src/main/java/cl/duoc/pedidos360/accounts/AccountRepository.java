package cl.duoc.pedidos360.accounts;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByIdentityKey(String key);
    Optional<Account> findByVerificationHash(String hash);
    org.springframework.data.domain.Page<Account> findByNombreContainingIgnoreCaseOrEmailContainingIgnoreCase(
        String nombre, String email, org.springframework.data.domain.Pageable pageable);
}
