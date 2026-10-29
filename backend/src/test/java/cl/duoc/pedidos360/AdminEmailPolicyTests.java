package cl.duoc.pedidos360;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import cl.duoc.pedidos360.security.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminEmailPolicyTests {
    private final AdminIdentityRepository identities = mock(AdminIdentityRepository.class);
    private final AdminEmailPolicy policy = new AdminEmailPolicy(identities);
    private Jwt token(String email, String tenant, String oid) {
        return Jwt.withTokenValue("test").header("alg", "RS256").subject(oid)
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
            .claim("preferred_username", email).claim("tid", tenant).claim("oid", oid).build();
    }
    private static final String ID = "11111111-1111-1111-1111-111111111111";
    @Test void caseAndWhitespaceDoNotChangeApprovedIdentity() {
        when(identities.findById(anyString())).thenReturn(Optional.empty());
        assertTrue(policy.isAdmin(token("  BENJAMINJEREZ@PEDIDOS360830.ONMICROSOFT.COM  ", AdminEmailPolicy.TENANT, ID)));
    }
    @Test void guestDirectoryNamesResolveToApprovedDuocAddresses() {
        assertEquals("alva.perezz@duocuc.cl", policy.email(token("alva.perezz_duocuc.cl#EXT#@pedidos360830.onmicrosoft.com", AdminEmailPolicy.TENANT, ID)));
        assertEquals("ne.gonzalezm@duocuc.cl", policy.email(token("ne.gonzalezm_duocuc.cl#EXT#@pedidos360830.onmicrosoft.com", AdminEmailPolicy.TENANT, ID)));
    }
    @Test void lookalikeEmailsCannotGainAdmin() {
        assertFalse(policy.isAdmin(token("alva.perezz@duocuc.cl.attacker.example", AdminEmailPolicy.TENANT, ID)));
        assertFalse(policy.isAdmin(token("not-alva.perezz@duocuc.cl", AdminEmailPolicy.TENANT, ID)));
        verifyNoInteractions(identities);
    }
    @Test void differentTenantOrMissingDirectoryIdCannotGainAdmin() {
        assertFalse(policy.isAdmin(token("alva.perezz@duocuc.cl", "another-tenant", ID)));
        assertFalse(policy.isAdmin(token("alva.perezz@duocuc.cl", AdminEmailPolicy.TENANT, "invalid-id")));
        verifyNoInteractions(identities);
    }
}
