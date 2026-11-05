package uz.zarflow.shared.security;

import java.time.Clock;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    void rejectsSecretShorterThan32Bytes() {
        assertThatThrownBy(() -> jwtConfig.jwtSigningKey("too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void acceptsSecretOfAtLeast32Bytes() {
        String secret = "a".repeat(32);

        SecretKey key = jwtConfig.jwtSigningKey(secret);

        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
    }

    @Test
    void encoderAndDecoderAreUsableWithTheSameKey() {
        SecretKey key = jwtConfig.jwtSigningKey("a".repeat(32));

        JwtEncoder encoder = jwtConfig.jwtEncoder(key);
        JwtDecoder decoder = jwtConfig.jwtDecoder(key, Clock.systemUTC());

        assertThat(encoder).isNotNull();
        assertThat(decoder).isNotNull();
    }

}
