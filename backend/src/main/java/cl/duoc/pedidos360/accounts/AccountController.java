package cl.duoc.pedidos360.accounts;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AccountController {
    private final AccountService accounts;
    private final CustomerSessions sessions;
    private final org.springframework.core.task.AsyncTaskExecutor recoveryExecutor;
    private final Map<String, Bucket> limits = new HashMap<>();
    private record Bucket(int count, Instant expires) {}
    @org.springframework.beans.factory.annotation.Value("${spring.mail.host:localhost}")
    private String smtpHost;
    public AccountController(AccountService accounts, CustomerSessions sessions,
            @org.springframework.beans.factory.annotation.Qualifier("recoveryExecutor") org.springframework.core.task.AsyncTaskExecutor recoveryExecutor) {
        this.accounts = accounts; this.sessions = sessions; this.recoveryExecutor = recoveryExecutor;
    }

    public record Register(@NotBlank @Size(max = 120) String nombre, @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 72) String password) {}
    public record Login(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 72) String password) {}
    public record EmailAddress(@NotBlank @Email @Size(max = 254) String email) {}
    public record Verification(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token) {}
    public record PasswordReset(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
            @NotBlank @Size(min = 12, max = 72) String password) {}

    @GetMapping("/public/auth/mail-mode")
    public Map<String, Object> mailMode() {
        return Map.of("mode", Set.of("localhost", "127.0.0.1", "mailpit").contains(smtpHost) ? "LOCAL" : "SMTP", "passwordResetAvailable", true);
    }

    @PostMapping("/public/auth/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody EmailAddress body, HttpServletRequest request) {
        limit("reset-request:" + request.getRemoteAddr(), 20);
        limit("reset-email:" + body.email().trim().toLowerCase(Locale.ROOT), 5);
        // Run every lookup off the request thread, including unknown and Microsoft-only addresses.
        try {
            recoveryExecutor.execute(() -> {
                try { accounts.requestPasswordReset(body.email()); }
                catch (Exception failure) {
                    org.slf4j.LoggerFactory.getLogger(AccountController.class).warn("No se pudo procesar un correo de recuperación; revisa SMTP.");
                }
            });
        } catch (org.springframework.core.task.TaskRejectedException full) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio está ocupado. Inténtalo más tarde.");
        }
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(Map.of("message",
            "Si existe una cuenta verificada con ese correo, recibirás un enlace para recuperar tu contraseña. Revisa también spam."));
    }

    @PostMapping("/public/auth/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordReset body, HttpServletRequest request) {
        limit("reset-attempt:" + request.getRemoteAddr(), 30);
        accounts.resetPassword(body.token(), body.password());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("message", "Contraseña actualizada. Inicia sesión de nuevo con tu nueva contraseña."));
    }

    @PostMapping("/public/auth/register")
    public ResponseEntity<?> register(@Valid @RequestBody Register body, HttpServletRequest request) {
        limit("register:" + request.getRemoteAddr(), 20);
        limit("mail:" + body.email().trim().toLowerCase(Locale.ROOT), 5);
        accounts.register(body.nombre(), body.email(), body.password());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(Map.of("message", "Si el correo puede registrarse, recibirás un enlace de verificación. Revisa también spam."));
    }

    @PostMapping("/public/auth/resend")
    public Map<String, String> resend(@Valid @RequestBody EmailAddress body, HttpServletRequest request) {
        limit("register:" + request.getRemoteAddr(), 20);
        limit("mail:" + body.email().trim().toLowerCase(Locale.ROOT), 5);
        accounts.resend(body.email());
        return Map.of("message", "Si tu cuenta está pendiente, recibirás un nuevo enlace de verificación.");
    }

    @PostMapping("/public/auth/verify")
    public Map<String, String> verify(@Valid @RequestBody Verification body, HttpServletRequest request) {
        limit("verify:" + request.getRemoteAddr(), 60); accounts.verify(body.token());
        return Map.of("message", "Correo verificado. Ya puedes iniciar sesión.");
    }

    @PostMapping("/public/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody Login body, HttpServletRequest request) {
        limit("login:" + request.getRemoteAddr(), 60);
        limit("login-email:" + body.email().trim().toLowerCase(Locale.ROOT), 20);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(accounts.login(body.email(), body.password()));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(JwtAuthenticationToken authentication) {
        sessions.revoke(authentication.getToken().getTokenValue());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/usuarios")
    public ResponseEntity<?> people(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "") String q) {
        var people = accounts.people(page, q);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("personas", people.getContent(),
            "pagina", people.getNumber(), "totalPaginas", people.getTotalPages(), "totalPersonas", people.getTotalElements()));
    }

    private synchronized void limit(String key, int max) {
        Instant now = Instant.now();
        limits.entrySet().removeIf(entry -> !entry.getValue().expires().isAfter(now));
        Bucket bucket = limits.getOrDefault(key, new Bucket(0, now.plusSeconds(3600)));
        if (bucket.count() >= max || limits.size() >= 4096)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Vuelve a intentarlo más tarde.");
        limits.put(key, new Bucket(bucket.count() + 1, bucket.expires()));
    }
}
