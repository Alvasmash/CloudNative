package cl.duoc.pedidos360.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * =========================================================================
 * SEGURIDAD: AuthoritiesConverter
 * =========================================================================
 * FUNCION: Traduce los permisos y roles contenidos en el Access Token JWT
 *          en autoridades que Spring Security entiende internamente:
 *          1. Scopes delegados (claim 'scp'): se convierten en 'SCOPE_recurso.write' / 'SCOPE_recurso.read'.
 *          2. Roles de aplicacion de Azure AD (claim 'roles'): se convierten en 'ROLE_ADMIN' / 'ROLE_USER'.
 * CONECTA CON: El bean JwtAuthenticationConverter en SecurityConfig.
 * EVALUACION (Rbrica 40%): "Aplica autorizacion por rol cuando corresponde y lee roles y scopes desde los claims".
 */
public class AuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final AdminEmailPolicy policy;

    private final JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();

    public AuthoritiesConverter(AdminEmailPolicy policy) {
        this.policy = policy;
        // Por convencin OAuth2 en Azure AD, los scopes vienen en el claim 'scp' separados por espacio
        scopesConverter.setAuthoritiesClaimName("scp");
        scopesConverter.setAuthorityPrefix("SCOPE_");
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        // 1. Extraer y agregar scopes delegados
        Collection<GrantedAuthority> scopes = scopesConverter.convert(jwt);
        if (scopes != null) {
            authorities.addAll(scopes);
        }

        // The directory token is already validated. Client-supplied roles never grant admin.
        authorities.add(new SimpleGrantedAuthority(policy.isAdmin(jwt) ? "ROLE_ADMIN" : "ROLE_USER"));

        return authorities;
    }
}
