package uz.zarflow.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zarflow")
public record ZarflowProperties(String defaultTimezone) {
}
