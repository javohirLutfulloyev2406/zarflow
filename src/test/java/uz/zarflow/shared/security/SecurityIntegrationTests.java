package uz.zarflow.shared.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import uz.zarflow.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private Clock clock;

    @Test
    void noTokenIsUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity("/probe/secured", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validTokenReturnsOkWithCorrectUserId() {
        UUID userId = UUID.randomUUID();
        String token = token(jwtEncoder, userId, "zarflow", Duration.ofMinutes(5));

        ResponseEntity<SecuredProbeController.SecuredResponse> response = restTemplate.exchange(
                RequestEntity.get("/probe/secured").header("Authorization", "Bearer " + token).build(),
                SecuredProbeController.SecuredResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().userId()).isEqualTo(userId.toString());
    }

    @Test
    void expiredTokenIsUnauthorized() {
        String token = token(jwtEncoder, UUID.randomUUID(), "zarflow", Duration.ofSeconds(-120));

        ResponseEntity<String> response = restTemplate.exchange(
                RequestEntity.get("/probe/secured").header("Authorization", "Bearer " + token).build(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void wrongIssuerIsUnauthorized() {
        String token = token(jwtEncoder, UUID.randomUUID(), "someone-else", Duration.ofMinutes(5));

        ResponseEntity<String> response = restTemplate.exchange(
                RequestEntity.get("/probe/secured").header("Authorization", "Bearer " + token).build(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenSignedWithDifferentSecretIsUnauthorized() {
        SecretKey otherKey = new SecretKeySpec(
                "a-completely-different-32-byte-secret!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(otherKey));

        String token = token(otherEncoder, UUID.randomUUID(), "zarflow", Duration.ofMinutes(5));

        ResponseEntity<String> response = restTemplate.exchange(
                RequestEntity.get("/probe/secured").header("Authorization", "Bearer " + token).build(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String token(JwtEncoder encoder, UUID subject, String issuer, Duration ttlFromNow) {
        Instant now = Instant.now(clock);
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject.toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttlFromNow))
                .build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

}
