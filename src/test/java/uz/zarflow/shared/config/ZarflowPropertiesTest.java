package uz.zarflow.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ZarflowPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class);

    @Test
    void bindsDefaultTimezoneFromProperty() {
        contextRunner
                .withPropertyValues("zarflow.default-timezone=Asia/Tashkent")
                .run(context -> assertThat(context.getBean(ZarflowProperties.class).defaultTimezone())
                        .isEqualTo("Asia/Tashkent"));
    }

    @EnableConfigurationProperties(ZarflowProperties.class)
    private static class PropertiesConfig {
    }

}
