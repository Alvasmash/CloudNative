package cl.duoc.pedidos360.security;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import com.sun.net.httpserver.HttpServer;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jwt.*;
import static org.junit.jupiter.api.Assertions.*;

class MicrosoftJwtDecoderTests {
    private static RSAKey key;
    private static HttpServer server;
    private JwtDecoder decoder;
    private static final String AUDIENCE = "aef48164-1df8-49e6-b9f2-c1d8953ea200";
    private static final String ISSUER = "https://login.microsoftonline.com/" + AdminEmailPolicy.TENANT + "/v2.0";

    @BeforeAll static void keys() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/keys", exchange -> {
            byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
    }
    @AfterAll static void stop() { server.stop(0); }
    @BeforeEach void decoder() {
        decoder = new LocalAuthConfig().localJwtDecoder(ISSUER, AUDIENCE,
            "http://127.0.0.1:" + server.getAddress().getPort() + "/keys");
    }
    private String token(String issuer, String audience, String tenant, Instant expires, RSAKey signingKey) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(issuer).audience(audience)
            .subject("subject").issueTime(Date.from(Instant.now().minusSeconds(120)))
            .expirationTime(Date.from(expires)).claim("tid", tenant)
            .claim("oid", "11111111-1111-1111-1111-111111111111").claim("scp", "recurso.write").build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), claims);
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }
    @Test void validSignedDelegatedMicrosoftApiTokenIsAccepted() throws Exception {
        assertNotNull(decoder.decode(token(ISSUER, AUDIENCE, AdminEmailPolicy.TENANT, Instant.now().plusSeconds(300), key)));
        assertNotNull(decoder.decode(token(ISSUER, "api://" + AUDIENCE, AdminEmailPolicy.TENANT, Instant.now().plusSeconds(300), key)));
    }
    @Test void differentSignatureIsRejected() throws Exception {
        String invalid = token(ISSUER, AUDIENCE, AdminEmailPolicy.TENANT, Instant.now().plusSeconds(300), new RSAKeyGenerator(2048).generate());
        assertThrows(JwtException.class, () -> decoder.decode(invalid));
    }
    @Test void wrongIssuerAudienceAndTenantAreRejected() throws Exception {
        for (String invalid : List.of(
            token("https://another-issuer.example", AUDIENCE, AdminEmailPolicy.TENANT, Instant.now().plusSeconds(300), key),
            token(ISSUER, "spa-id-token-audience", AdminEmailPolicy.TENANT, Instant.now().plusSeconds(300), key),
            token(ISSUER, AUDIENCE, "another-tenant", Instant.now().plusSeconds(300), key))) {
            assertThrows(JwtException.class, () -> decoder.decode(invalid));
        }
    }
    @Test void expiredAndOldDemoTokensAreRejected() throws Exception {
        String expired = token(ISSUER, AUDIENCE, AdminEmailPolicy.TENANT, Instant.now().minusSeconds(300), key);
        assertThrows(JwtException.class, () -> decoder.decode(expired));
        assertThrows(JwtException.class, () -> decoder.decode("old-local-demo-token"));
    }
}
