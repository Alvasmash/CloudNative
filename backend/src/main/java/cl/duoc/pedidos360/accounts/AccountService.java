package cl.duoc.pedidos360.accounts;

import cl.duoc.pedidos360.security.AdminEmailPolicy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {
    private final AccountRepository accounts;
    private final CustomerSessions sessions;
    private final JavaMailSender mail;
    private final AdminEmailPolicy policy;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(12);
    private final String dummyPassword = passwords.encode("not-a-real-account-password");
    private final FrontendLinks frontendLinks;
    private final String sender;

    public AccountService(AccountRepository accounts, CustomerSessions sessions, JavaMailSender mail,
            AdminEmailPolicy policy, FrontendLinks frontendLinks,
            @Value("${app.accounts.mail-from:registro@pedidos360.local}") String sender) {
        this.accounts = accounts; this.sessions = sessions; this.mail = mail; this.policy = policy;
        this.frontendLinks = frontendLinks; this.sender = sender;
    }

    public record Person(String id, String nombre, String email, String proveedor, boolean verificado,
            String rol, Instant fechaRegistro, Instant ultimoAcceso) {}
    private Person person(Account a) {
        return new Person(a.id, a.nombre, a.email, a.proveedor, a.verificado,
            a.administrador && "MICROSOFT".equals(a.proveedor) ? "ADMIN" : "USER", a.fechaRegistro, a.ultimoAcceso);
    }

    @Transactional(readOnly = true)
    public Page<Person> people(int page, String query) {
        var pageable = PageRequest.of(Math.max(0, page), 25, Sort.by(Sort.Direction.DESC, "fechaRegistro"));
        String q = query.trim();
        if (q.length() > 120) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La búsqueda es demasiado larga.");
        return (q.isBlank() ? accounts.findAll(pageable) : accounts.findByNombreContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q, pageable)).map(this::person);
    }

    @Transactional
    public void register(String nombre, String email, String password) {
        email = normalize(email);
        if (policy.isReservedEmail(email)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este correo debe iniciar sesión con Microsoft.");
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe ocupar como máximo 72 bytes.");
        Account account = accounts.findByIdentityKey("local:" + email).orElse(null);
        if (account != null) return; // Do not reveal account existence or replace a pending password.
        account = new Account();
        account.identityKey = "local:" + email; account.nombre = nombre.trim(); account.email = email;
        account.proveedor = "CORREO"; account.passwordHash = passwords.encode(password);
        sendVerification(account);
        accounts.save(account);
    }

    @Transactional
    public void resend(String email) {
        accounts.findByIdentityKey("local:" + normalize(email)).filter(a -> !a.verificado).ifPresent(a -> {
            sendVerification(a); accounts.save(a);
        });
    }

    private void sendVerification(Account account) {
        String token = CustomerSessions.randomToken();
        account.verificationHash = CustomerSessions.hash(token);
        account.verificationExpires = Instant.now().plusSeconds(24 * 3600);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender); message.setTo(account.email); message.setSubject("Verifica tu correo en Pedidos360");
        message.setText("Hola " + account.nombre + ",\n\nConfirma tu correo para activar tu cuenta en Pedidos360:\n\n"
            + frontendLinks.verification(token)
            + "\n\nEl enlace vence en 24 horas. Si no solicitaste esta cuenta, ignora este mensaje.\n\nPedidos360");
        try { mail.send(message); }
        catch (MailException unavailable) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo enviar el correo. Inténtalo más tarde.");
        }
    }

    @Transactional
    public void verify(String token) {
        Account account = accounts.findByVerificationHash(CustomerSessions.hash(token)).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace no es válido o ya fue utilizado."));
        if (account.verificationExpires == null || !account.verificationExpires.isAfter(Instant.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace ha caducado. Solicita un nuevo correo.");
        account.verificado = true; account.verificationHash = null; account.verificationExpires = null;
    }

    @Transactional
    public Map<String, Object> login(String email, String password) {
        Account account = accounts.findByIdentityKey("local:" + normalize(email)).orElse(null);
        boolean valid = passwords.matches(password, account == null ? dummyPassword : account.passwordHash);
        if (!valid || account == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        if (!account.verificado) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Verifica tu correo antes de iniciar sesión.");
        account.ultimoAcceso = Instant.now();
        return sessions.create(account);
    }

    @Transactional
    public synchronized void recordMicrosoft(Jwt jwt, boolean admin) {
        if (!AdminEmailPolicy.TENANT.equals(jwt.getClaimAsString("tid")) || jwt.getClaimAsString("oid") == null) return;
        String key = "microsoft:" + jwt.getClaimAsString("tid") + ":" + jwt.getClaimAsString("oid");
        Account a = accounts.findByIdentityKey(key).orElseGet(Account::new);
        a.identityKey = key; a.email = policy.email(jwt); a.proveedor = "MICROSOFT";
        String name = jwt.getClaimAsString("name");
        a.nombre = (name == null || name.isBlank() ? a.email : name).substring(0, Math.min(120, (name == null || name.isBlank() ? a.email : name).length()));
        a.verificado = true; a.administrador = admin; a.ultimoAcceso = Instant.now();
        accounts.save(a);
    }

    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
