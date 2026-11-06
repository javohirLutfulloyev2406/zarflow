package uz.zarflow.shared.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

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

import uz.zarflow.IntegrationTest;
import uz.zarflow.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiDisabledByDefaultIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private Clock clock;

    @Test
    void apiDocsAreNotFoundEvenWhenAuthenticated() {
        ResponseEntity<String> response = restTemplate.exchange(
                RequestEntity.get("/v3/api-docs").header("Authorization", "Bearer " + token()).build(),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void apiDocsAreUnauthorizedWithoutAToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String token() {
        Instant now = Instant.now(clock);
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("zarflow")
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(5)))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

}
