package cl.duoc.pedidos360.controller;

import java.util.Map;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import cl.duoc.pedidos360.security.AdminEmailPolicy;

@RestController
public class SessionProfileController {
    private final AdminEmailPolicy policy;
    public SessionProfileController(AdminEmailPolicy policy) { this.policy = policy; }
    @GetMapping("/api/session")
    public Map<String, Object> session(JwtAuthenticationToken authentication) {
        String email = policy.email(authentication.getToken());
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String name = authentication.getToken().getClaimAsString("name");
        return Map.of("name", name == null || name.isBlank() ? email : name,
            "email", email, "username", authentication.getName(),
            "isAdmin", admin, "roles", java.util.List.of(admin ? "ADMIN" : "USER"));
    }
}
