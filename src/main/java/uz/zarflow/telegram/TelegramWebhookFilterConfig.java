package uz.zarflow.telegram;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
class TelegramWebhookFilterConfig {

    @Bean
    FilterRegistrationBean<TelegramWebhookGuardFilter> telegramWebhookGuardFilter(TelegramBotProperties properties) {
        FilterRegistrationBean<TelegramWebhookGuardFilter> registration =
                new FilterRegistrationBean<>(new TelegramWebhookGuardFilter(properties));
        registration.addUrlPatterns(TelegramWebhookController.WEBHOOK_PATH);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

}
