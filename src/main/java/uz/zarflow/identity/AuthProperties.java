package uz.zarflow.identity;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "zarflow.auth")
record AuthProperties(
        String botToken,
        @DefaultValue("PT24H") Duration initDataMaxAge,
        @DefaultValue("PT1H") Duration jwtTtl) {
}
