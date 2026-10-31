package cl.duoc.pedidos360.accounts;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByIdentityKey(String key);
    Optional<Account> findByVerificationHash(String hash);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Account a where a.identityKey = :key")
    Optional<Account> findByIdentityKeyForUpdate(@org.springframework.data.repository.query.Param("key") String key);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Account a where a.resetHash = :hash")
    Optional<Account> findByResetHashForUpdate(@org.springframework.data.repository.query.Param("hash") String hash);
    org.springframework.data.domain.Page<Account> findByNombreContainingIgnoreCaseOrEmailContainingIgnoreCase(
        String nombre, String email, org.springframework.data.domain.Pageable pageable);
}
