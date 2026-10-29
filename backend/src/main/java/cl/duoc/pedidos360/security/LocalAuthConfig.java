package cl.duoc.pedidos360.security;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;

/** Local API, with identity verified against Microsoft Entra ID. No demo tokens. */
@Configuration
@Profile("local")
public class LocalAuthConfig {
    @Bean
    JwtDecoder localJwtDecoder(
            @Value("${app.security.issuer}") String issuer,
            @Value("${app.security.audience}") String audience,
            @Value("${app.security.jwk-set-uri:https://login.microsoftonline.com/16c2a7cf-f000-475f-a847-f95ee0b36404/discovery/v2.0/keys}") String keys) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(keys).build();
        OAuth2TokenValidator<Jwt> identity = token -> {
            boolean valid = token.getAudience().stream().anyMatch(Set.of(audience, "api://" + audience)::contains)
                && AdminEmailPolicy.TENANT.equals(token.getClaimAsString("tid"))
                && token.getClaimAsString("oid") != null
                && token.getClaimAsString("scp") != null;
            return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Token de usuario o audiencia no valida", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), identity));
        return decoder;
    }
}
