package cl.duoc.pedidos360.security;

import java.util.Locale;
import java.util.Set;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AdminEmailPolicy {
    public static final String TENANT = "16c2a7cf-f000-475f-a847-f95ee0b36404";
    private static final Set<String> ADMINS = Set.of(
        "benjaminjerez@pedidos360830.onmicrosoft.com", "alva.perezz@duocuc.cl", "ne.gonzalezm@duocuc.cl");
    private final AdminIdentityRepository identities;
    public AdminEmailPolicy(AdminIdentityRepository identities) { this.identities = identities; }

    public String email(Jwt token) {
        for (String claim : new String[]{"preferred_username", "upn", "email"}) {
            String value = token.getClaimAsString(claim);
            if (value != null && !value.isBlank()) {
                String normalized = value.trim().toLowerCase(Locale.ROOT);
                for (String approved : ADMINS) {
                    String guestUpn = approved.replace('@', '_') + "#ext#@pedidos360830.onmicrosoft.com";
                    if (normalized.equals(guestUpn)) return approved;
                }
                return normalized;
            }
        }
        return "";
    }

    public synchronized boolean isAdmin(Jwt token) {
        String email = email(token);
        String objectId = token.getClaimAsString("oid");
        if (!TENANT.equals(token.getClaimAsString("tid")) || !ADMINS.contains(email)
                || objectId == null || !objectId.matches("(?i)[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")) return false;
        var binding = identities.findById(email);
        if (binding.isPresent()) return binding.get().getObjectId().equalsIgnoreCase(objectId);
        try {
            identities.saveAndFlush(new AdminIdentityBinding(email, objectId.toLowerCase(Locale.ROOT)));
            return true;
        } catch (org.springframework.dao.DataIntegrityViolationException conflict) {
            return identities.findById(email).map(row -> row.getObjectId().equalsIgnoreCase(objectId)).orElse(false);
        }
    }
}
