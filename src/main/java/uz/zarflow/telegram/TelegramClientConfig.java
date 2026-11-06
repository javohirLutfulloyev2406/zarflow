package uz.zarflow.telegram;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
class TelegramClientConfig {

    @Bean
    TelegramClient telegramClient(TelegramBotProperties properties) {
        return new OkHttpTelegramClient(properties.botToken());
    }

}
