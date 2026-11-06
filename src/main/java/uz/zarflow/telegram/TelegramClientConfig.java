package uz.zarflow.telegram;

import okhttp3.OkHttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
class TelegramClientConfig {

    @Bean
    TelegramClient telegramClient(TelegramBotProperties properties) {
        // callTimeout bounds the whole request (connect + write + read), so a hanging
        // or slow Telegram API response can never block the caller indefinitely.
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(properties.connectTimeout())
                .callTimeout(properties.callTimeout())
                .build();
        return new OkHttpTelegramClient(httpClient, properties.botToken());
    }

}
