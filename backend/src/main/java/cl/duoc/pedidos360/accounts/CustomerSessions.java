package cl.duoc.pedidos360.accounts;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Random bearer credentials, stored only as SHA-256 hashes and revocable on logout. */
@Service
public class CustomerSessions {
    public static final String ISSUER = "urn:pedidos360:accounts";
    private final CustomerSessionRepository sessions;
    public CustomerSessions(CustomerSessionRepository sessions) { this.sessions = sessions; }

    @Transactional
    public Map<String, Object> create(Account account) {
        sessions.deleteByExpiresAtBefore(Instant.now());
        String token = "p360_" + randomToken();
        CustomerSession session = new CustomerSession();
        session.tokenHash = hash(token);
        session.account = account;
        session.expiresAt = Instant.now().plusSeconds(8 * 3600);
        sessions.save(session);
        return Map.of("token", token, "expiresAt", session.expiresAt, "name", account.nombre,
            "email", account.email, "username", "local:" + account.id, "isAdmin", false,
            "roles", List.of("USER"));
    }

    @Transactional(readOnly = true)
    public Jwt decode(String token) {
        if (!token.matches("p360_[A-Za-z0-9_-]{43}")) throw new BadJwtException("Sesión no válida");
        CustomerSession session = sessions.findById(hash(token)).orElseThrow(() -> new BadJwtException("Sesión no válida"));
        if (!session.expiresAt.isAfter(Instant.now()) || !session.account.verificado)
            throw new BadJwtException("La sesión ha caducado");
        Account account = session.account;
        return Jwt.withTokenValue(token).header("alg", "SESSION").issuer(ISSUER)
            .subject("local:" + account.id).claim("oid", "local:" + account.id)
            .claim("email", account.email).claim("name", account.nombre)
            .claim("scp", "recurso.read recurso.write").issuedAt(session.expiresAt.minusSeconds(8 * 3600))
            .expiresAt(session.expiresAt).build();
    }

    @Transactional
    public void revoke(String token) { if (token.startsWith("p360_")) sessions.deleteById(hash(token)); }

    public static String randomToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
