package uz.zarflow.telegram;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

@Component
@ConditionalOnProperty(prefix = "zarflow.telegram", name = "register-webhook", havingValue = "true")
class TelegramBotStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotStartupRunner.class);

    private static final List<BotCommand> COMMANDS_UZ = List.of(
            new BotCommand("start", "Botni ishga tushirish"),
            new BotCommand("app", "ZarFlow ilovasini ochish"),
            new BotCommand("help", "Yordam"));

    private static final List<BotCommand> COMMANDS_RU = List.of(
            new BotCommand("start", "Запустить бота"),
            new BotCommand("app", "Открыть приложение ZarFlow"),
            new BotCommand("help", "Помощь"));

    private final TelegramGateway gateway;
    private final TelegramBotProperties properties;

    TelegramBotStartupRunner(TelegramGateway gateway, TelegramBotProperties properties) {
        this.gateway = gateway;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        tryRun("setWebhook", () -> gateway.setWebhook(
                properties.webhookUrl() + "/telegram/webhook",
                properties.webhookSecret(),
                List.of("message", "callback_query"),
                false));
        tryRun("setChatMenuButton", () -> gateway.setChatMenuButton("ZarFlow", properties.miniAppUrl()));
        tryRun("setMyCommands(uz/default)", () -> gateway.setMyCommands(COMMANDS_UZ, null));
        tryRun("setMyCommands(ru)", () -> gateway.setMyCommands(COMMANDS_RU, "ru"));
    }

    private void tryRun(String step, Runnable action) {
        try {
            action.run();
        } catch (TelegramGatewayException ex) {
            log.error("Telegram bot startup step failed: {}", step);
        }
    }

}
