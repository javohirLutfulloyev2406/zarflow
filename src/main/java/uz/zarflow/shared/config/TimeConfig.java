package uz.zarflow.shared.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TimeConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ZoneId defaultZoneId(ZarflowProperties properties) {
        return ZoneId.of(properties.defaultTimezone());
    }

}
