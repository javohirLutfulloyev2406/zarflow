package uz.zarflow.shared.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * One place owning the JWT signing key, so issuing ({@code identity}'s login flow)
 * and verifying (every other request, via {@link SecurityConfig}) always agree on
 * algorithm and key material. Fails fast at startup if the secret is too short to
 * be a real HS256 key, not on the first request that happens to need it.
 */
@Configuration
public class JwtConfig {

    private static final String ISSUER = "zarflow";
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(30);
    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    SecretKey jwtSigningKey(@Value("${zarflow.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "zarflow.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes, got " + bytes.length);
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        JWKSource<SecurityContext> jwkSource = new ImmutableSecret<>(jwtSigningKey);
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(CLOCK_SKEW);
        timestampValidator.setClock(clock);

        OAuth2TokenValidator<Jwt> issuerValidator = new JwtClaimValidator<>(JwtClaimNames.ISS, ISSUER::equals);

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestampValidator, issuerValidator));
        return decoder;
    }

}
